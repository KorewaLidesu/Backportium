package surreal.backportium.core.transformers;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public abstract class BasicTransformer implements Opcodes {

    protected static MethodInsnNode hook(String name, String desc) {
        return new MethodInsnNode(INVOKESTATIC, "surreal/backportium/core/BPHooks", name, desc, false);
    }

    protected static String getName(String mcpName, String srgName) {
        return FMLLaunchHandler.isDeobfuscatedEnvironment() ? mcpName : srgName;
    }

    protected static ClassNode read(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassNode cls = new ClassNode();
        reader.accept(cls, 0);
        return cls;
    }

    protected static byte[] write(ClassNode cls) {
        return write(cls, ClassWriter.COMPUTE_MAXS);
    }

    protected static byte[] write(ClassNode cls, int options) {
        ClassWriter writer = new ClassWriter(options);
        cls.accept(writer);
        return writer.toByteArray();
    }

    protected static void writeClass(ClassNode cls) {
        if (!FMLLaunchHandler.isDeobfuscatedEnvironment()) return;
        File file = new File("classOut/" + cls.name + ".class");
        file.getParentFile().mkdirs();
        try (OutputStream stream = Files.newOutputStream(file.toPath())) {
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cls.accept(writer);
            stream.write(writer.toByteArray());
        } catch (IOException ignored) {
        }
    }

    protected static MethodNode clinit(ClassNode cls) {
        return cls.methods.get(cls.methods.size() - 1);
    }

    protected static IntList getDescList(String desc) {
        IntList list = new IntArrayList();
        boolean inPar = false;
        int parType = -1;
        for (int i = 1; i < desc.length(); i++) {
            char c = desc.charAt(i);
            if (c == ')') return list;
            if (!inPar) {
                if (c == 'L' || c == '[') {
                    parType = ALOAD;
                    inPar = true;
                }
                else {
                    switch (c) {
                        case 'Z':
                        case 'I': parType = ILOAD; break;
                        case 'F': parType = FLOAD; break;
                        case 'D': parType = DLOAD; break;
                        case 'J': parType = LLOAD; break;
                    }
                }
            }
            else if (c == ';') inPar = false;
            if (!inPar) {
                list.add(parType);
            }
        }
        System.out.println(desc + "  to  " + list);
        return list;
    }

    protected static boolean isSuper(ClassNode cls, String superName) {
        if (cls.superName.equals("java/lang/Object")) return false;
        else if (cls.superName.equals(superName)) return true;
        try {
            Class<?> clazz = Class.forName(cls.superName.replace('/', '.'));
            return isSuper(clazz, superName);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isSuper(Class<?> cls, String superName) {
        Class<?> superCls = cls.getSuperclass();
        if (superCls.getName().equals("java.lang.Object")) return false;
        if (superCls.getName().replace('.', '/').equals(superName)) return true;
        else return isSuper(superCls, superName);
    }

    protected static void loadNewClass(String clsName, byte[] basicClass) {
        LaunchClassLoader loader = (LaunchClassLoader) BasicTransformer.class.getClassLoader();
        File out = new File(".backportium_classes");
        Path path = Paths.get(out.getAbsolutePath(), clsName + ".jar");
        File f = path.toFile();
        f.getParentFile().mkdirs();
        try {
            JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(path)));
            jos.putNextEntry(new JarEntry(clsName + ".class"));
            jos.write(basicClass);
            jos.closeEntry();
            jos.close();
            loader.addURL(path.toUri().toURL());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}