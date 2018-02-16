// Copyright (C) 2018 ScyllaDB
// Use of this source code is governed by a ALv2-style
// license that can be found in the LICENSE file.

package com.scylladb.driver.tools.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

class OptimizingTransformer implements ClassFileTransformer {

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> redefiningClass, ProtectionDomain domain,
			byte[] classByteCode) throws IllegalClassFormatException {
		if (!"com/datastax/driver/core/Cluster$Builder".equals(className)) {
			return null;
		}
		ClassPool pool = ClassPool.getDefault();
		CtClass cl = null;
		try {
			cl = pool.makeClass(new java.io.ByteArrayInputStream(classByteCode));

			CtMethod method = cl.getMethod("build", "()Lcom/datastax/driver/core/Cluster;");
			method.setBody("{ System.out.println(\"Creating optimized cluster...\"); return com.scylladb.driver.tools.ClusterOptimizer.buildWithPagingOptimized(this); }");
			classByteCode = cl.toBytecode();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cl != null) {
				cl.detach();
			}
		}
		return classByteCode;
	}

}
