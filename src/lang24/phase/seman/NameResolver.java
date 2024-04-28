package lang24.phase.seman;

import lang24.common.report.*;
import lang24.data.ast.tree.*;
import lang24.data.ast.tree.defn.*;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.type.*;
import lang24.data.ast.visitor.*;

/**
 * Name resolver.
 * 
 * The name resolver connects each node of a abstract syntax tree where a name
 * is used with the node where it is defined. The only exceptions are struct and
 * union component names which are connected with their definitions by type
 * resolver. The results of the name resolver are stored in
 * {@link lang24.phase.seman.SemAn#definedAt}.
 */

public class NameResolver implements AstFullVisitor<Object, NameResolver.Context> {

	/** Constructs a new name resolver. */
	public NameResolver() {
	}

	/** The symbol table. */
	private SymbTable symbTable = new SymbTable();

	private boolean start = true;
	public enum Context{
		FST_RUN, SND_RUN
	}

	@Override
	public Object visit(AstNodes<? extends AstNode> nodes, Context arg) {
		if (start) {
			symbTable.newScope();
			start = false;
		}
		boolean first = true;
		boolean unscope = false;
		//boolean components = false;
		for(Context context: new Context[]{Context.FST_RUN, Context.SND_RUN}){
			for (AstNode n : nodes){
				if(n instanceof AstDefn){
					if (n instanceof AstRecType.AstCmpDefn){
						unscope = true;
					}
					if(first && unscope && context == Context.FST_RUN){
						symbTable.newScope();
						first = false;
					}
					n.accept(this, context);
				} else if (context == Context.SND_RUN) {
					n.accept(this, null);
				}
			}
		}
		if(unscope) {
			symbTable.oldScope();
		}

		return null;
	}

	@Override
	public Object visit(AstTypDefn typDefn, Context arg) {
		if(typDefn.type != null) {
			if (arg == Context.FST_RUN){
                try {
                    symbTable.ins(typDefn.name, typDefn);
                } catch (SymbTable.CannotInsNameException e) {
					throw new Report.Error(typDefn, "Type "
							+ typDefn.name + " already defined.");
                }
            } else if (arg == Context.SND_RUN) {
				typDefn.type.accept(this, null);
			}
		}
		return null;
	}

	@Override
	public Object visit(AstVarDefn varDefn, Context arg) {
		if (varDefn.type != null) {
			if (arg == Context.FST_RUN){
				try {
					symbTable.ins(varDefn.name, varDefn);
				} catch (SymbTable.CannotInsNameException e) {
					throw new Report.Error(varDefn, "Variable "
							+ varDefn.name + " already defined.");
				}
			} else if (arg == Context.SND_RUN) {
				varDefn.type.accept(this, null);
			}
		}
		return null;
	}

	@Override
	public Object visit(AstFunDefn funDefn, Context arg) {
		if (arg == Context.FST_RUN){
            try {
                symbTable.ins(funDefn.name, funDefn);
            } catch (SymbTable.CannotInsNameException e) {
				throw new Report.Error(funDefn, "Function "
						+ funDefn.name + " already defined.");
            }
        } else if (arg == Context.SND_RUN) {
			// Same scope as function
			if (funDefn.type != null) {funDefn.type.accept(this, null);}

			symbTable.newScope();
			if (funDefn.pars != null) {funDefn.pars.accept(this, null);}

			symbTable.newScope();
			if(funDefn.defns != null) {funDefn.defns.accept(this, null);}
			if (funDefn.stmt != null) {funDefn.stmt.accept(this, null);}
			symbTable.oldScope();

			symbTable.oldScope();
		}
		return null;
	}

	@Override
	public Object visit(AstFunDefn.AstRefParDefn refParDefn, Context arg) {
		if(refParDefn.type != null){
			if (arg == Context.FST_RUN){
                try {
                    symbTable.ins(refParDefn.name, refParDefn);
                } catch (SymbTable.CannotInsNameException e) {
					throw new Report.Error(refParDefn, "Parameter name "
							+ refParDefn.name + " already defined.");
                }
            } else if (arg == Context.SND_RUN) {
				refParDefn.type.accept(this, null);
			}
		}
		return null;
	}

	@Override
	public Object visit(AstFunDefn.AstValParDefn valParDefn, Context arg) {
		if(valParDefn.type != null){
			if (arg == Context.FST_RUN){
				try {
					symbTable.ins(valParDefn.name, valParDefn);
				} catch (SymbTable.CannotInsNameException e) {
					throw new Report.Error(valParDefn, "Parameter "
							+ valParDefn.name + " already defined.");
				}
			} else if (arg == Context.SND_RUN) {
				valParDefn.type.accept(this, null);
			}
		}
		return null;
	}

	@Override
	public Object visit(AstNameType nameType, Context arg) {
        try {
            SemAn.definedAt.put(nameType, symbTable.fnd(nameType.name));
        } catch (SymbTable.CannotFndNameException e) {
			throw new Report.Error(nameType, "Type "
					+ nameType.name + " not found.");
        }
        return null;
	}

	@Override
	public Object visit(AstRecType.AstCmpDefn cmpDefn, Context arg) {
		if(cmpDefn.type != null){
			if (arg == Context.FST_RUN){
				try {
					symbTable.ins(cmpDefn.name, cmpDefn);
				} catch (SymbTable.CannotInsNameException e) {
					throw new Report.Error(cmpDefn, "Component "
							+ cmpDefn.name + " already defined.");
				}
			} else if (arg == Context.SND_RUN) {
				cmpDefn.type.accept(this, null);
			}
		}
		return null;
	}

	@Override
	public Object visit(AstCallExpr callExpr, Context arg) {
		try {
			SemAn.definedAt.put(callExpr, symbTable.fnd(callExpr.name));
		} catch (SymbTable.CannotFndNameException e) {
			throw new Report.Error(callExpr, "Function "
					+ callExpr.name + " not found.");
		}

		if (callExpr.args != null) {
			callExpr.args.accept(this, null);
		}

		return null;
	}

	@Override
	public Object visit(AstNameExpr nameExpr, Context arg) {
        try {
            SemAn.definedAt.put(nameExpr, symbTable.fnd(nameExpr.name));
        } catch (SymbTable.CannotFndNameException e) {
			throw new Report.Error(nameExpr, "Name "
					+ nameExpr.name + " not found.");
        }
        return null;
	}

	@Override
	public Object visit(AstCmpExpr cmpExpr, Context arg) {
		if (cmpExpr.expr != null) {
			cmpExpr.expr.accept(this, null);
		}
		return null;
    }
}