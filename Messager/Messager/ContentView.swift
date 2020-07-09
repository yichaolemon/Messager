//
//  ContentView.swift
//  Messager
//
//  Created by Yichao Cheng on 6/29/20.
//  Copyright © 2020 Plusheen. All rights reserved.
//

import SwiftUI

class SenderDelegateObject: ObservableObject, SenderDelegate {
    func onError(_ err: Error) {
        self.errMessage = err.localizedDescription
    }
    
    func receivedMessage(_ message: String) {
        self.receivedMessage = message
    }
    
    @Published var errMessage: String?
    @Published var receivedMessage: String?
}

struct ContentView: View {
    @ObservedObject var senderDelegate = SenderDelegateObject()
    
    @State private var message: String = ""
    @State private var sentMessage: String = ""
    @State private var hasLoggedIn: Bool = false
    
    private unowned var sender: Sender!
    
    init() {
    }
    
    init(sender: Sender) {
        self.sender = sender
        self.sender.delegate = self.senderDelegate
    }
    
    func sendMessage() {
        sentMessage = message
        self.sender.handle(input: sentMessage)
        message = ""
    }
    
    var body: some View {
        if !hasLoggedIn {
            return AnyView(LoginView(sender: sender, hasLoggedIn: $hasLoggedIn))
        } else {
            return AnyView(HStack {
                Text(senderDelegate.receivedMessage ?? "").frame(maxWidth: .infinity, maxHeight: .infinity)
                VStack {
                    Text(senderDelegate.errMessage ?? "").frame(maxWidth: .infinity, maxHeight: .infinity)
                    Text(sentMessage).frame(maxWidth: .infinity, maxHeight: .infinity)
                    TextField("Write a message...", text: $message, onCommit: {
                        self.sendMessage()
                    })
                }
            })
        }
    }
}


struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
