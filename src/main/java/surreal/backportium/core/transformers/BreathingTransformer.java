package surreal.backportium.core.transformers;

import org.objectweb.asm.tree.*;

import java.util.Iterator;

public class BreathingTransformer extends BasicTransformer {

    /**
     * Handle 1.13 entity breathing
     * @param cls Comes from {@link TridentTransformer#transformEntityLivingBase(byte[])}
     **/
    public static byte[] transformEntityLivingBase(ClassNode cls) {
        for (MethodNode method : cls.methods) {
            if (method.name.equals(getName("onEntityUpdate", "func_70030_z"))) {
                Iterator<AbstractInsnNode> iterator = method.instructions.iterator();
                while (iterator.hasNext()) {
                    AbstractInsnNode node = iterator.next();
                    if (node.getOpcode() == SIPUSH) {
                        InsnList list = new InsnList();
                        list.add(new VarInsnNode(ALOAD, 0));
                        list.add(new MethodInsnNode(INVOKEVIRTUAL, cls.name, getName("getAir", "func_70086_ai"), "()I", false));
                        list.add(new InsnNode(ICONST_4));
                        list.add(new InsnNode(IADD));
                        list.add(new IntInsnNode(SIPUSH, 300));
                        list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Math", "min", "(II)I", false));
                        method.instructions.insertBefore(node, list);
                        method.instructions.remove(node);
                        break;
                    }
                }
                break;
            }
        }
        return write(cls);
    }

    public static byte[] transformGuiIngameForge(byte[] basicClass) {
        ClassNode cls = read(basicClass);
        for (MethodNode method : cls.methods) {
            if (method.name.equals("renderAir")) {
                Iterator<AbstractInsnNode> iterator = method.instructions.iterator();
                while (iterator.hasNext()) {
                    AbstractInsnNode node = iterator.next();
                    if (node.getOpcode() == ALOAD && ((VarInsnNode) node).var == 3) {
                        InsnList list = new InsnList();
                        list.add(new VarInsnNode(ALOAD, 3));
                        list.add(new MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/entity/player/EntityPlayer", getName("getAir", "func_70086_ai"), "()I", false));
                        list.add(new IntInsnNode(SIPUSH, 300));
                        method.instructions.insertBefore(node, list);
                        for (int i = 0; i < 3; i++) {
                            node = node.getNext();
                            method.instructions.remove(node.getPrevious());
                        }
                        ((JumpInsnNode) node).setOpcode(IF_ICMPEQ);
                        break;
                    }
                }
                break;
            }
        }
        return write(cls);
    }
}
