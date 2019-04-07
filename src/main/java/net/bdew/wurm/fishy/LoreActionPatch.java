package net.bdew.wurm.fishy;

import javassist.CannotCompileException;
import javassist.CtMethod;
import javassist.bytecode.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import net.bdew.wurm.tools.server.bytecode.ByteCodeUtils;

public class LoreActionPatch {
    public static void apply(CtMethod loreMethod, int tickRate) throws BadBytecode, CannotCompileException {
        MethodInfo mi = loreMethod.getMethodInfo();
        CodeAttribute ca = mi.getCodeAttribute();
        ConstPool constPool = ca.getConstPool();
        CodeIterator codeIterator = ca.iterator();

        int check = 0;

        while (codeIterator.hasNext()) {
            int pos = codeIterator.next();
            if (codeIterator.byteAt(pos) == Bytecode.INVOKEVIRTUAL) {
                int ref = codeIterator.u16bitAt(pos + 1);
                if (constPool.getMethodrefName(ref).equals("getSecond")) {
                    int constPos = pos = codeIterator.next();
                    int num = ByteCodeUtils.getInteger(constPool, codeIterator, pos);
                    pos = codeIterator.next();
                    int constSz = pos - constPos;
                    check++;

                    Bytecode changed = new Bytecode(constPool);
                    ByteCodeUtils.putInteger(constPool, changed, check * tickRate);

                    FishyMod.logInfo(String.format("Patching %s Check for %d seconds at pos=%d sz=%d - replacing with %d newSz=%d", loreMethod.getName(), num, constPos, constSz, check * tickRate, changed.getSize()));

                    if (changed.getSize() > constSz)
                        codeIterator.insertGapAt(constPos, changed.getSize() - constSz, true);
                    else while (changed.getSize() < constSz)
                        changed.add(Bytecode.NOP);

                    codeIterator.write(changed.get(), constPos);
                }
            }
        }

        int finalTime = check * tickRate * 10;

        loreMethod.instrument(new ExprEditor() {
            private int gft = 0;

            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("setTimeLeft")) {
                    m.replace(String.format("$proceed(%d);", finalTime));
                    FishyMod.logInfo(String.format("Patching %s - changed setTimeLeft to %d at %d", loreMethod.getName(), finalTime, m.getLineNumber()));
                } else if (m.getMethodName().equals("sendActionControl")) {
                    m.replace(String.format("$proceed($1, $2, %d);", finalTime));
                    FishyMod.logInfo(String.format("Patching %s - changed sendActionControl to %d at %d", loreMethod.getName(), finalTime, m.getLineNumber()));
                } else if (m.getMethodName().equals("getFishTable") && gft++ == 1) {
                    m.replace("$_ = $proceed($$); if (net.bdew.wurm.fishy.Hooks.maybeShowFishTable(performer, source, $_, tileX, tileY)) return true;");
                    FishyMod.logInfo(String.format("Patching %s - added full table hook at %d", loreMethod.getName(), m.getLineNumber()));
                }
            }
        });
    }
}
