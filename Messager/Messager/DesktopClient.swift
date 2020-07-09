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
    case serverError(String)
    case unrecognizedServerResponse(String)
}

protocol SenderDelegate {
    func onError(_ err: Error)
    func receivedMessage(_ message: String)
}

// informs user of new groups they have joined
protocol SenderGroupDelegate {
    func joinedGroup(_ groupName: String)
    func hasNewContact(_ username: String)
}

class Sender: NSObject, StreamDelegate {
    var inputStream: InputStream?
    var outputStream: OutputStream?
    var host: String
    var port: Int
    var delegate: SenderDelegate?
    var groupDelegate: SenderGroupDelegate?
    
    private var inRepl = true
    private var currentGroup: String?
    
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
//                self.delegate?.receivedMessage(readline())
                handleReceived(data: readline())
            }
            break
        default:
            break
        }
    }
    
    func handleReceived(data: String) {
        let components: [Substring] = data.split(separator: "|")
        switch components[0] {
        case "message":
            let groupName = components[1]
            let senderAndTimeInfo = components[2]
            let encryptedMsg = components[3]
            // TODO: decrypt this message
            if String(groupName) == self.currentGroup {
                self.delegate?.receivedMessage(String(senderAndTimeInfo) + encryptedMsg)
            }
        case "error":
            let errorMsg = components[1]
            self.delegate?.onError(SenderError.serverError(String(errorMsg)))
        case "public keys":
            for i in 1..<components.count {
                if i%2 == 1 {
                    self.groupDelegate?.hasNewContact(String(components[i]))
                }
            }
        case "group key":
            let groupName = components[1]
            self.groupDelegate?.joinedGroup(String(groupName))
        default:
            self.delegate?.onError(SenderError.unrecognizedServerResponse(data))
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
    
    private var readBuffer: String = ""
    private func readline() -> String {
        var output = ""
        while true {
            if readBuffer.isEmpty {
                readBuffer = read()
            }
            if let range: Range<String.Index> = readBuffer.range(of: "\n") {
                output.append(contentsOf: readBuffer.prefix(upTo: range.lowerBound))
                readBuffer = String(readBuffer.suffix(from: range.upperBound))
                return output
            } else {
                output.append(readBuffer)
                readBuffer.removeAll(keepingCapacity: true)
            }
        }
    }
    
    func handle(input: String) {
        if inRepl {
            handle(command: input)
        } else {
            handle(message: input)
        }
    }
    
    func login(username: String, password: String) {
        write("login|\(username)|\(password)|TODO public key\n")
    }
    
    let loginPattern = NSRegularExpression("^\\s*login\\s+(\\w+)\\s+(\\S+)\\s*$")
    let createGroupPattern = NSRegularExpression("^\\s*create\\s+group\\s+(\\d+)\\s+(\\w+(,\\s*\\w+)*)\\s*$")
    let enterGroupPattern = NSRegularExpression("^\\s*enter\\s+group\\s+(\\d+)\\s*$")
    let helpPattern = NSRegularExpression("^\\s*help\\s*$")
    let commaPattern = NSRegularExpression("\\s*,\\s*")
    
    private func handle(command: String) {
        if let loginMatch = loginPattern.matchGroups(command) {
            login(username: loginMatch[1], password: loginMatch[2])
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
            let groupId = enterGroupMatch[1]
            // TODO: check AES key for group
            enter(group: groupId)
        } else if helpPattern.matchGroups(command) != nil {
            // TODO: report help info somewhere the user can see
            print("Available commands: login, create group, enter group")
        } else {
            self.delegate?.onError(SenderError.invalidInput)
        }
    }
    
    private func enter(group: String) {
        self.inRepl = false
        self.currentGroup = group
        let timestamp = 0  // TODO: remember the last timestamp for each group
        write("fetch|\(group)|\(timestamp)\n")
    }
    
    private func handle(message: String) {
        if let groupName = currentGroup {
            write("message|\(groupName)|\(message)\n")
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


