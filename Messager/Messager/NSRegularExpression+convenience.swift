//
//  NSRegularExpression+convenience.swift
//  Messager
//
//  Created by Yichao Cheng on 7/7/20.
//  Copyright © 2020 Plusheen. All rights reserved.
//

import Foundation

extension NSRegularExpression {
    convenience init(_ pattern: String) {
        do {
            try self.init(pattern: pattern)
        } catch {
            preconditionFailure("Illegal regular expression: \(pattern).")
        }
    }
    
    func matchGroups(_ string: String) -> [String]? {
        let nsstring = NSString(string: string)
        let result = self.firstMatch(in: string, options: [], range: NSRange(location: 0, length: nsstring.length))
        if result == nil {
            return nil
        }
        var results = [String]()
        for i in 0..<result!.numberOfRanges {
            results.append(nsstring.substring(with: result!.range(at: i)))
        }
        return results
    }
    
    func split(_ string: String) -> [String] {
        let nsstring = NSString(string: string)
        let result = self.firstMatch(in: string, options: [], range: NSRange(location: 0, length: nsstring.length))
        if result == nil {
            return [string]
        }
        let range = result!.range(at: 0)
        let endOfFirst = range.location
        let startOfRest = range.location + range.length
        if startOfRest == 0 {
            return [string]
        }
        return [nsstring.substring(to: endOfFirst)] + split(nsstring.substring(from: startOfRest))
    }
}
