//
//  SidebarView.swift
//  Messager
//
//  Created by Yichao Cheng on 7/7/20.
//  Copyright © 2020 Plusheen. All rights reserved.
//

import SwiftUI

class SidebarViewDataSource: ObservableObject {
    // list of groups
    @Published var listOfGroups : [String] = []
    init() {
        
    }
    
}

struct SidebarView: View {
    @ObservedObject var senderDelegate = SidebarViewDataSource()
    private unowned var sender: Sender!
    
    init(sender: Sender) {
        self.sender = sender
    }
    
    var body: some View {
        List(0..<5) { item in
            VStack(alignment: .leading) {
                Text("Simon Ng")
            }
            .frame(width: nil, height: 40.0)
        }
        .frame(width: 230.0)
    }
}

struct SidebarView_Previews: PreviewProvider {
    static var previews: some View {
        SidebarView()
    }
}
