//
//  DesktopClient.swift
//  Messager
//
//  Created by Yichao Cheng on 6/29/20.
//  Copyright © 2020 Plusheen. All rights reserved.
//

import Foundation

enum SenderError: Error {
    case notInGroup
    case invalidInput
}

protocol SenderDelegate {
    func onError(_ err: Error)
    func receivedMessage(_ message: String)
}

class Sender: NSObject, StreamDelegate {
    var inputStream: InputStream?
    var outputStream: OutputStream?
    var host: String
    var port: Int
    var delegate: SenderDelegate?
    
    private var inRepl = true
    private var currentGroup: Int?
    
    init(host: String, port: Int) {
        self.host = host
        self.port = port
    }
    
    func stream(_ aStream: Stream, handle eventCode: Stream.Event) {
        switch eventCode {
        case .errorOccurred:
            print("an error occurred on stream \(aStream)")
            break
        case .hasBytesAvailable:
            if aStream == inputStream {
                self.delegate?.receivedMessage(read())
            }
            break
        default:
            break
        }
    }
    
    func connectToServer() {
        Stream.getStreamsToHost(withName: self.host, port: self.port, inputStream: &self.inputStream, outputStream: &self.outputStream)
        /*CFStreamCreatePairWithSocketToHost(kCFAllocatorDefault,
                                           self.host as CFString,
                                           self.port,
                                           &readStream,
                                           &writeStream)*/
        //inputStream = readStream!.takeRetainedValue()
        //outputStream = writeStream!.takeRetainedValue()
        if inputStream != nil && outputStream != nil {
            inputStream!.schedule(in: .main, forMode: .default)
            outputStream!.schedule(in: .main, forMode: .default)
            inputStream!.delegate = self
            outputStream!.delegate = self
            inputStream!.open()
            outputStream!.open()
        }
    }
    
    private let bufferSize = 10
    
    private func read() -> String {
        var output = ""
        var buffer = [UInt8](repeating: 0, count: bufferSize)
        while self.inputStream!.hasBytesAvailable {
            let bytesRead: Int = inputStream!.read(&buffer, maxLength: buffer.count)
            output += NSString(bytes: buffer, length: bytesRead, encoding: String.Encoding.ascii.rawValue)! as String
        }
        return output
    }
    
    func handle(input: String) {
        if inRepl {
            handle(command: input)
        } else {
            handle(message: input)
        }
    }
    
    let loginPattern = NSRegularExpression("^\\s*login\\s+(\\w+)\\s+(\\S+)\\s*$")
    let createGroupPattern = NSRegularExpression("^\\s*create\\s+group\\s+(\\d+)\\s+(\\w+(,\\s*\\w+)*)\\s*$")
    let enterGroupPattern = NSRegularExpression("^\\s*enter\\s+group\\s+(\\d+)\\s*$")
    let helpPattern = NSRegularExpression("^\\s*help\\s*$")
    let commaPattern = NSRegularExpression("\\s*,\\s*")
    
    private func handle(command: String) {
        if let loginMatch = loginPattern.matchGroups(command) {
            let username = loginMatch[1]
            let password = loginMatch[2]
            write("login|\(username)|\(password)|TODO public key\n")
        } else if let createGroupMatch = createGroupPattern.matchGroups(command) {
            let groupId = createGroupMatch[1]
            let usernameStr = createGroupMatch[2]
            let usernames = commaPattern.split(usernameStr);
            var msg = ""
            // TODO: generate AES keys
            for username in usernames {
                msg.append("|\(username)|key")
            }
            write("create group|\(groupId)\(msg)\n")
        } else if let enterGroupMatch = enterGroupPattern.matchGroups(command) {
            guard let groupId = Int(enterGroupMatch[1]) else {
                self.delegate?.onError(SenderError.invalidInput)
                return
            }
            // TODO: check AES key for group
            enter(group: groupId)
        } else if helpPattern.matchGroups(command) != nil {
            // TODO: report help info somewhere the user can see
            print("Available commands: login, create group, enter group")
        } else {
            self.delegate?.onError(SenderError.invalidInput)
        }
    }
    
    private func enter(group: Int) {
        self.inRepl = false
        self.currentGroup = group
        let timestamp = 0  // TODO: remember the last timestamp for each group
        write("fetch|\(group)|\(timestamp)\n")
    }
    
    private func handle(message: String) {
        if let groupId = currentGroup {
            write("message|\(groupId)|\(message)\n")
        } else {
            self.delegate?.onError(SenderError.notInGroup)
        }
    }
    
    private func write(_ str: String) {
        debugPrint("writing", str)
        let data = str.data(using: .utf8)!
        _ = data.withUnsafeBytes({ (bytes) -> Void in
            let pointer = bytes.baseAddress!.assumingMemoryBound(to: UInt8.self)
            self.outputStream!.write(pointer, maxLength: data.count)
        })
    }
}


