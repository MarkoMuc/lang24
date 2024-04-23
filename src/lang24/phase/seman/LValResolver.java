package lang24.phase.seman;

import lang24.common.report.*;
import lang24.data.ast.tree.AstNode;
import lang24.data.ast.tree.AstNodes;
import lang24.data.ast.tree.defn.AstDefn;
import lang24.data.ast.tree.defn.AstFunDefn;
import lang24.data.ast.tree.defn.AstVarDefn;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.AstAssignStmt;
import lang24.data.ast.visitor.*;
/**
 * Lvalue resolver.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
/*
	FIXME:
	 1. Check all throw conditions
 */

public class LValResolver implements AstFullVisitor<Object, Object> {

	/** Constructs a new lvalue resolver. */
	public LValResolver() {
	}

	@Override
	public Object visit(AstNameExpr nameExpr, Object arg) {
		// Is this get LVal?
		AstDefn defn = SemAn.definedAt.get(nameExpr);
		if(defn instanceof AstVarDefn ||
				defn instanceof AstFunDefn.AstParDefn){
			SemAn.isLVal.put(nameExpr, true);
		}

		return null;
	}

	@Override
	public Object visit(AstArrExpr arrExpr, Object arg) {
		arrExpr.arr.accept(this, null);
		arrExpr.idx.accept(this, null);
		Boolean test = SemAn.isLVal.get(arrExpr.arr);

		if(test != null && test){
			SemAn.isLVal.put(arrExpr, true);
		}else {
			throw new Report.Error(arrExpr,"L-value Array Error ");
		}
		return null;
	}

	@Override
	public Object visit(AstCmpExpr cmpExpr, Object arg) {
		cmpExpr.expr.accept(this, null);
		Boolean test = SemAn.isLVal.get(cmpExpr.expr);
		if(test != null && test){
			SemAn.isLVal.put(cmpExpr, true);
		}
		return null;
	}

	@Override
	public Object visit(AstSfxExpr sfxExpr, Object arg) {
		sfxExpr.expr.accept(this, null);
		SemAn.isLVal.put(sfxExpr, true);
		return null;
	}

	@Override
	public Object visit(AstCastExpr castExpr, Object arg) {
		castExpr.expr.accept(this, null);
		Boolean test = SemAn.isLVal.get(castExpr.expr);
		if(test != null && test){
			SemAn.isLVal.put(castExpr, true);
		}

		return null;
	}

	@Override
	public Object visit(AstPfxExpr pfxExpr, Object arg) {
		//TODO : check if correct
		pfxExpr.expr.accept(this, null);
		Boolean test = SemAn.isLVal.get(pfxExpr.expr);
		if(test != null && test){
			SemAn.isLVal.put(pfxExpr, true);
		}else if(pfxExpr.oper == AstPfxExpr.Oper.PTR ) {
			throw new Report.Error(pfxExpr,"L-value prefix Error ");
		}

		return null;
	}

	@Override
	public Object visit(AstCallExpr callExpr, Object arg) {
		// FIXME:
		//  1. Add L-val checking
		// 	2. Can this be just callExpr.args.accept()?
		// 		-> Depends if I want to check L-val here or in type resolver
		if(callExpr.args != null){
			for(AstExpr expr : callExpr.args){
				expr.accept(this, null);
			}
		}
		return null;
	}

	@Override
	public Object visit(AstFunDefn funDefn, Object arg) {
		funDefn.type.accept(this, null);
		if(funDefn.pars != null) funDefn.pars.accept(this, null);
		if(funDefn.defns != null) funDefn.defns.accept(this, null);
		if(funDefn.stmt != null) funDefn.stmt.accept(this, null);

		return null;
	}

	@Override
	public Object visit(AstAssignStmt assignStmt, Object arg) {
		assignStmt.dst.accept(this,null);
		assignStmt.src.accept(this,null);
		Boolean test = SemAn.isLVal.get(assignStmt.dst);

		if(test == null || !test){
			throw new Report.Error(assignStmt,"L-value Error ");
		}
		return null;
	}

	@Override
	public Object visit(AstNodes<? extends AstNode> nodes, Object arg) {
		for(AstNode n: nodes){
			n.accept(this, null);
		}
		return null;
	}

	@Override
	public Object visit(AstFunDefn.AstRefParDefn refParDefn, Object arg) {
		// FIXME: L-val check
		SemAn.isLVal.put(refParDefn, true);
		return null;
	}

	@Override
	public Object visit(AstFunDefn.AstValParDefn valParDefn, Object arg) {
		SemAn.isLVal.put(valParDefn, true);
		return null;
	}

	@Override
	public Object visit(AstVarDefn varDefn, Object arg) {
		SemAn.isLVal.put(varDefn, true);
		return null;
	}
}
