package com.github.knokko.bitser.test;

import sun.reflect.ReflectionFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;

public class ReflectionPlayground {

	public static class ExampleClass implements Serializable {

		final String name;
		final int x;

		ExampleClass(String name, int x) {
			this.name = name;
			this.x = x;
			System.out.println("Invoked proper constructor");
		}

		public ExampleClass() {
			this.name = null;
			this.x = 0;
			System.out.println("Invoked fake constructor");
		}
	}

	static void main() throws Throwable {
		var factory = ReflectionFactory.getReflectionFactory();
		var exampleConstructor = factory.newConstructorForSerialization(ExampleClass.class, ExampleClass.class.getDeclaredConstructor());
		var exampleInstance = (ExampleClass) exampleConstructor.newInstance();
		var exampleReader = factory.defaultReadObjectForSerialization(ExampleClass.class);
		System.out.println(exampleInstance.x);
		exampleReader.invoke(exampleInstance, new ObjectInputStream() {

			@Override
			public ObjectInputStream.GetField readFields() throws IOException {
				System.out.println("readFields");
				return new ObjectInputStream.GetField() {

					@Override
					public ObjectStreamClass getObjectStreamClass() {
						return null;
					}

					@Override
					public boolean defaulted(String s) throws IOException {
						return false;
					}

					@Override
					public boolean get(String s, boolean b) throws IOException {
						return false;
					}

					@Override
					public byte get(String s, byte b) throws IOException {
						return 0;
					}

					@Override
					public char get(String s, char c) throws IOException {
						return 0;
					}

					@Override
					public short get(String s, short i) throws IOException {
						return 0;
					}

					@Override
					public int get(String s, int i) throws IOException {
						System.out.println("get int " + i);
						return 5;
					}

					@Override
					public long get(String s, long l) throws IOException {
						return 0;
					}

					@Override
					public float get(String s, float v) throws IOException {
						return 0;
					}

					@Override
					public double get(String s, double v) throws IOException {
						return 0;
					}

					@Override
					public Object get(String s, Object o) throws IOException, ClassNotFoundException {
						return null;
					}
				};
			}
		});

		System.out.println(exampleInstance.x);
	}
}
