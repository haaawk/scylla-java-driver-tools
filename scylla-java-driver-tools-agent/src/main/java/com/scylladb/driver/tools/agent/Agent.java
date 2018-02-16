// Copyright (C) 2018 ScyllaDB
// Use of this source code is governed by a ALv2-style
// license that can be found in the LICENSE file.

package com.scylladb.driver.tools.agent;

import java.lang.instrument.Instrumentation;

public class Agent {
	public static void premain(String args, Instrumentation instrumentation) {
		System.out.println("Running with cluster optimizing agent...");
		instrumentation.addTransformer(new OptimizingTransformer());
	}
}
