package lang24.phase.vecan;

import lang24.data.ast.attribute.Attribute;
import lang24.data.ast.tree.stmt.AstVecForStmt;
import lang24.data.datadep.LoopDescriptor;
import lang24.phase.Phase;

public class VecAn extends Phase {

    public final static Attribute<AstVecForStmt, LoopDescriptor> loopDescriptors = new Attribute<>();

    public VecAn(){
        super("vecan");
    }

}
