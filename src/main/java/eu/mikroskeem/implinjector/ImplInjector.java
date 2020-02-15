/*
 * This file is part of project ImplInjector, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020 Mark Vainomaa <mikroskeem@mikroskeem.eu>
 * Copyright (c) Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package eu.mikroskeem.implinjector;

import org.checkerframework.checker.nullness.qual.NonNull;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * Interface static field setting utility
 *
 * @author Mark Vainomaa
 */
public final class ImplInjector {
    /**
     * Injects an interface instance into static field of given interface
     *
     * @param interfaceClass Interface class where to inject the instance
     * @param fieldName Interface field name
     * @param instance Interface implementation instance
     * @param <T> Interface type
     */
    public static <T> void inject(Class<T> interfaceClass, @NonNull String fieldName, @NonNull T instance) {
        Objects.requireNonNull(interfaceClass, "Interface class shouldn't be null");
        Objects.requireNonNull(fieldName, "Field name should not be null");
        Objects.requireNonNull(instance, "Instance should not be null");

        try {
            if (modifiersVarHandle != null) {
                inject1(interfaceClass, fieldName, instance);
            } else if (unsafe != null) {
                inject0(interfaceClass, fieldName, instance);
            } else {
                throw new RuntimeException("Unable to find a suitable way for injecting implementation into the interface");
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            unsafe.throwException(e);
        }
    }

    private static void inject0(Class<?> interfaceClass, String fieldName, Object instance) throws NoSuchFieldException {
        Field f = interfaceClass.getDeclaredField(fieldName);
        f.setAccessible(true);
        Object base = unsafe.staticFieldBase(f);
        long off = unsafe.staticFieldOffset(f);
        unsafe.putObject(base, off, instance);
    }

    private static void inject1(Class<?> interfaceClass, String fieldName, Object instance) throws IllegalAccessException, NoSuchFieldException {
        Field f = interfaceClass.getDeclaredField(fieldName);
        f.setAccessible(true);
        ((VarHandle) modifiersVarHandle).set(f, f.getModifiers() & ~Modifier.FINAL);
        f.set(null, instance);
    }

    private static final Unsafe unsafe;
    private static final Object/*VarHandle*/ modifiersVarHandle;

    private static Unsafe initUnsafeField() {
        try {
            Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            return (Unsafe) theUnsafeField.get(null);
        } catch (IllegalAccessException | NoSuchFieldException ignored) {}
        return null;
    }

    private static Object initModifiersVarHandle() {
        try {
            VarHandle.class.getName(); // Makes this method fail-fast on JDK 8
            return MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup())
                    .findVarHandle(Field.class, "modifiers", int.class);
        } catch (IllegalAccessException | NoClassDefFoundError | NoSuchFieldException ignored) {}
        return null;
    }

    static {
        unsafe = initUnsafeField();
        modifiersVarHandle = initModifiersVarHandle();
    }
}
