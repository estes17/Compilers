package visitor;

import syntaxtree.*;
import errorMsg.*;

// The purpose of this class is to detect and report unreachable code,
// according to Java's rules.
public class Sem5Visitor extends ASTvisitor {
	
	ConstEvalVisitor conEval;
	ErrorMsg errorMsg;
	boolean sawBreak;
	
	public Sem5Visitor(ErrorMsg e) {
		initInstanceVars(e);
	}

	private void initInstanceVars(ErrorMsg e) {
		errorMsg = e;
		conEval = new ConstEvalVisitor();
		sawBreak = false;
	}
}
	
