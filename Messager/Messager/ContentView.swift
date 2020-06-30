//
//  ContentView.swift
//  Messager
//
//  Created by Yichao Cheng on 6/29/20.
//  Copyright © 2020 Plusheen. All rights reserved.
//

import SwiftUI

struct ContentView: View {
    @State private var message: String = ""
    @State private var sentMessage: String = ""
    
    func sendMessage() -> Void {
        sentMessage = message
    }
    
    var body: some View {
        HStack {
            Text("Your message").frame(maxWidth: .infinity, maxHeight: .infinity)
            VStack {
                Text(sentMessage).frame(maxWidth: .infinity, maxHeight: .infinity)
                TextField("Write a message...", text: $message, onCommit: {
                    self.sendMessage()
                })
            }
        }
    }
}


struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
