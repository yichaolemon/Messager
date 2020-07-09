//
//  LoginView.swift
//  Messager
//
//  Created by Yichao Cheng on 7/7/20.
//  Copyright © 2020 Plusheen. All rights reserved.
//

import SwiftUI

struct LoginView: View {
    @State var username: String = ""
    @State var password: String = ""
    @Binding var hasLoggedIn: Bool
    private unowned var sender: Sender!
    
    func login() {
        self.sender.login(username: username, password: password)
        self.hasLoggedIn = true
    }
    
    init() {
        self._hasLoggedIn = Binding<Bool>.constant(false)
    }
    
    init(sender: Sender, hasLoggedIn: Binding<Bool>) {
        self.sender = sender
        self._hasLoggedIn = hasLoggedIn
    }
    
    var body: some View {
        VStack() {
            Text("Messager")
                .font(.largeTitle)
                .padding(.bottom, 20.0)
            TextField("username", text: $username).padding(.top, 30.0).frame(width: 170.0).font(/*@START_MENU_TOKEN@*/.body/*@END_MENU_TOKEN@*/)
            SecureField("password", text: $password)
                .padding(.vertical, 20.0)
                .frame(width: 170.0)
                .font(/*@START_MENU_TOKEN@*/.body/*@END_MENU_TOKEN@*/)
                
                
            Button(action: {
                self.login()
            }) {
                Text("Log In")
                    .font(.body)
                    .multilineTextAlignment(.center)
                    .padding(.top, 20.0)
                    
                    
            }
            .frame(width: 50.0, height: 50.0)
            
        }
        .frame(width: 330.0, height: 450.0)
        .foregroundColor(/*@START_MENU_TOKEN@*/.white/*@END_MENU_TOKEN@*/)
        .background(Color(red: 0.4, green: 0.6, blue: 1.0, opacity: 1.0))
    }
}

struct LoginView_Previews: PreviewProvider {
    static var previews: some View {
        LoginView()
    }
}
