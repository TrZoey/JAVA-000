package com.prik.work01;

import java.io.*;
import java.lang.reflect.InvocationTargetException;

/**
 * @description: 自定义类加载器
 * @author: Xu Shiwei
 * @create: 2020-10-20
 **/
public class MyClassLoader extends ClassLoader {

    /**
     * 寻找文件的根路径
     */
    private final String rootPath;

    public MyClassLoader(String rootPath) {
        super();
        this.rootPath = rootPath;
    }

    /**
     * 重写 findClass 方法
     *
     * @param name
     * @return
     * @throws ClassNotFoundException
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {

        // 1. 找到文件，转换为字节流
        File file = new File(rootPath + File.separator + name);
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new ClassNotFoundException(name);
        }

        // 2. 对字节进行处理
        int value;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            while ((value = inputStream.read()) != -1) {
                outputStream.write(255 - value);
            }
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }

        // 3. 调用方法，定义一个类
        byte[] bytes = outputStream.toByteArray();
        return defineClass("Hello", bytes, 0, bytes.length);
    }

    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, NoSuchMethodException, InvocationTargetException {
        MyClassLoader myClassLoader = new MyClassLoader("Week_01");
        Class<?> hello = myClassLoader.loadClass("Hello.xlass");
        Object instance = hello.newInstance();
        hello.getMethod("hello").invoke(instance);
    }
}