//
//  AppDelegate.swift
//  Messager
//
//  Created by Yichao Cheng on 6/29/20.
//  Copyright © 2020 Plusheen. All rights reserved.
//

import Cocoa
import SwiftUI

@NSApplicationMain
class AppDelegate: NSObject, NSApplicationDelegate {

    var window: NSWindow!
    var sender: Sender!

    func applicationDidFinishLaunching(_ aNotification: Notification) {
        self.sender = Sender(host: "raspberrypi.local", port: 5100)
        // Create the SwiftUI view that provides the window contents.
        let contentView = ContentView(sender: self.sender)
        DispatchQueue.global().async {
            self.sender.connectToServer()
        }

        // Create the window and set the content view. 
        window = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 480, height: 300),
            styleMask: [.titled, .closable, .miniaturizable, .resizable, .fullSizeContentView],
            backing: .buffered, defer: false)
        window.center()
        window.setFrameAutosaveName("Main Window")
        window.contentView = NSHostingView(rootView: contentView)
        window.makeKeyAndOrderFront(nil)
    }

    func applicationWillTerminate(_ aNotification: Notification) {
        // Insert code here to tear down your application
    }


}

