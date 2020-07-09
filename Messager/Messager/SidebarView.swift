//
//  SidebarView.swift
//  Messager
//
//  Created by Yichao Cheng on 7/7/20.
//  Copyright © 2020 Plusheen. All rights reserved.
//

import SwiftUI

class SidebarViewDataSource: ObservableObject, SenderGroupDelegate {
    @Published var listOfGroups: [String] = []
    @Published var listOfContacts: [String] = []
    func joinedGroup(_ groupName: String) {
        listOfGroups.append(groupName)
    }
    
    func hasNewContact(_ username: String) {
        listOfContacts.append(username)
    }
    
    init() {
        
    }
    
}

struct SidebarView: View {
    @ObservedObject var senderDelegate = SidebarViewDataSource()
    private unowned var sender: Sender!
    
    init(sender: Sender) {
        self.sender = sender
        self.sender.groupDelegate = senderDelegate
    }
    
    init() {
    }
    
    var body: some View {
        let listOfGroups = senderDelegate.listOfGroups
        let listOfContacts = senderDelegate.listOfContacts
        return
            VStack {
                List(0..<listOfContacts.count, id: \.self) { item in
                    VStack(alignment: .leading) {
                        Text(listOfContacts[item])
                    }
                    .frame(width: nil, height: 40.0)
                }
                .frame(width: 230.0)
                List(0..<listOfGroups.count, id: \.self) { item in
                    VStack(alignment: .leading) {
                        Text(listOfGroups[item])
                    }
                    .frame(width: nil, height: 40.0)
                }
                .frame(width: 230.0)
            }
    }
}

struct SidebarView_Previews: PreviewProvider {
    static var previews: some View {
        SidebarView()
    }
}
