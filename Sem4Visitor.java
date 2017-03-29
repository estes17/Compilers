package visitor;

import syntaxtree.*;

import java.util.*;

import errorMsg.*;

// The purpose of this class is to do type-checking and related
// actions.  These include:
// - evaluate the type for each expression, filling in the 'type'
//   link for each
// - ensure that each expression follows MiniJava's type rules (e.g.,
//   that the arguments to '*' are both integer, the argument to
//   '.length' is an array, etc.)
// - ensure that each method-call follows Java's type rules:
//   - there exists a method for the given class (or a superclass)
//     for the receiver's object type
//   - the method has the correct number of parameters
//   - the type of each actual parameter is compatible with that
//     of its corresponding formal parameter
// - ensure that for each instance variable access (e.g., abc.foo),
//   there is an instance variable defined for the given class (or
//   in a superclass
//   - sets the 'varDec' link in the InstVarAccess to refer to the
//     method
// - ensure that the RHS expression in each assignment statement is
//   type-compatible with its corresponding LHS
//   - also checks that the LHS is an lvalue
// - ensure that if a method with a given name is defined in both
//   a subclass and a superclass, that they have the same parameters
//   (with identical types) and the same return type
// - ensure that the declared return-type of a method is compatible
//   with its "return" expression
// - ensuring that the type of the control expression for an if- or
//   while-statement is boolean
public class Sem4Visitor extends ASTvisitor {

	ClassDecl currentClass;
	IdentifierType currentClassType;
	IdentifierType currentSuperclassType;
	BooleanType theBoolType;
	IntegerType theIntType;
	NullType theNullType;
	VoidType theVoidType;
	IdentifierType theStringType;
	ErrorMsg errorMsg;
	Hashtable<String, ClassDecl> globalSymTab;

	public Sem4Visitor(Hashtable<String, ClassDecl> globalSymTb, ErrorMsg e) {
		globalSymTab = globalSymTb;
		errorMsg = e;
		initInstanceVars();
	}

	private void initInstanceVars() {
		currentClass = null;
		theBoolType = new BooleanType(-1);
		theIntType = new IntegerType(-1);
		theNullType = new NullType(-1);
		theVoidType = new VoidType(-1);
		theStringType = new IdentifierType(-1, "String");
		theStringType.link = globalSymTab.get("String");

	}

	private boolean matchTypesExact(Type have, Type need, int pos) {
		if (have == null || need == null) return false;
		if (have.equals(need)) return true;
		if (pos >= 0) errorMsg.error(pos, "Incompatible types");
		return false;
	}

	private boolean matchTypesAssign(Type src, Type target, int pos) {
		if (src == null || target == null) return false;
		if (src instanceof VoidType || target instanceof VoidType) {
			errorMsg.error(pos, "Incompatible types");
			return false;
		}
		if (src.equals(target)) return true;
		if (src instanceof NullType && (target instanceof IdentifierType || target instanceof ArrayType)) return true;
		if (src instanceof ArrayType && target instanceof IdentifierType && ((IdentifierType) target).name.equals("Object")) return true;
		if (src instanceof IdentifierType) {
			ClassDecl current = ((IdentifierType) src).link;

		}
		return false;
	}

	private boolean matchTypesEqCompare(Type t1, Type t2, int pos) {
		if (t1 == null || t2 == null) return false;
		if (matchTypesAssign(t1, t2, pos) && matchTypesAssign(t2, t1, -pos)) return true;
		if (pos >= 0) errorMsg.error(pos, "Incompatible types");
		return false;
	}

	private InstVarDecl instVarLookup(String name, ClassDecl clas, int pos, String msg) {
		ClassDecl current = clas;
		while (current != null) {
			if (current.instVarTable.containsKey(name)) return current.instVarTable.get(name);
			current = current.superLink;
		}
		errorMsg.error(pos, msg);
		return null;
	}

	private InstVarDecl instVarLookup(String name, Type t, int pos, String msg) {
		if (t == null) return null;
		if (!(t instanceof IdentifierType)) {
			errorMsg.error(pos, msg);
			return null;
		}
		final ClassDecl decl = ((IdentifierType) t).link;
		if (decl.instVarTable.containsKey(name)) return decl.instVarTable.get(name);
		return null;
	}

	private MethodDecl methodLookup(String name, ClassDecl clas, int pos, String msg) {
		ClassDecl current = clas;
		while (current != null) {
			if (current.methodTable.containsKey(name)) return current.methodTable.get(name);
			current = current.superLink;
		}
		errorMsg.error(pos, msg);
		return null;
	}

	private MethodDecl methodLookup(String name, Type t, int pos, String msg) {
		if (t == null) return null;
		if (!(t instanceof IdentifierType)) {
			errorMsg.error(pos, msg);
			return null;
		}
		final ClassDecl decl = ((IdentifierType) t).link;
		if (decl.methodTable.containsKey(name)) return decl.methodTable.get(name);
		return null;
	}

	private Type returnTypeFor(MethodDecl md) {
		if (md instanceof MethodDeclVoid) return theVoidType;
		return ((MethodDeclNonVoid) md).rtnType;
	}

	public Object visitIntegerLiteral(IntegerLiteral n) {
		super.visitIntegerLiteral(n);
		n.type = theIntType;
		return null;
	}

	public Object visitNull(Null n) {
		super.visitNull(n);
		n.type = theNullType;
		return null;
	}

	public Object visitStringLiteral(StringLiteral n) {
		super.visitStringLiteral(n);
		n.type = theStringType;
		return null;
	}

	public Object visitTrue(True n) {
		super.visitTrue(n);
		n.type = theBoolType;
		return null;
	}

	public Object visitFalse(False n) {
		super.visitFalse(n);
		n.type = theBoolType;
		return null;
	}

	public Object visitIdentifierExp(IdentifierExp n) {
		super.visitIdentifierExp(n);
		n.type = n.link.type;
		return null;
	}

	public Object visitThis(This n) {
		super.visitThis(n);
		n.type = currentClassType;
		return null;
	}

	public Object visitSuper(Super n) {
		super.visitSuper(n);
		n.type = currentSuperclassType;
		return null;
	}

	public Object visitPlus(Plus n) {
		super.visitPlus(n);
		matchTypesExact(n.left.type, theIntType, n.left.pos);
		matchTypesExact(n.right.type, theIntType, n.right.pos);
		n.type = theIntType;
		return null;
	}

	public Object visitMinus(Minus n) {
		super.visitMinus(n);
		matchTypesExact(n.left.type, theIntType, n.left.pos);
		matchTypesExact(n.right.type, theIntType, n.right.pos);
		n.type = theIntType;
		return null;
	}

	public Object visitTimes(Times n) {
		super.visitTimes(n);
		matchTypesExact(n.left.type, theIntType, n.left.pos);
		matchTypesExact(n.right.type, theIntType, n.right.pos);
		n.type = theIntType;
		return null;
	}

	public Object visitDivide(Divide n) {
		super.visitDivide(n);
		matchTypesExact(n.left.type, theIntType, n.left.pos);
		matchTypesExact(n.right.type, theIntType, n.right.pos);
		n.type = theIntType;
		return null;
	}

	public Object visitRemainder(Remainder n) {
		super.visitRemainder(n);
		matchTypesExact(n.left.type, theIntType, n.left.pos);
		matchTypesExact(n.right.type, theIntType, n.right.pos);
		n.type = theIntType;
		return null;
	}

	public Object visitGreaterThan(GreaterThan n) {
		super.visitGreaterThan(n);
		matchTypesExact(n.left.type, theIntType, n.left.pos);
		matchTypesExact(n.right.type, theIntType, n.right.pos);
		n.type = theBoolType;
		return null;
	}

	public Object visitLessThan(LessThan n) {
		super.visitLessThan(n);
		matchTypesExact(n.left.type, theIntType, n.left.pos);
		matchTypesExact(n.right.type, theIntType, n.right.pos);
		n.type = theBoolType;
		return null;
	}

	public Object visitEquals(Equals n) {
		super.visitEquals(n);
		matchTypesEqCompare(n.left.type, n.right.type, n.pos);
		n.type = theBoolType;
		return null;
	}

	public Object visitNot(Not n) {
		super.visitNot(n);
		matchTypesExact(n.exp.type, theBoolType, n.exp.pos);
		n.type = theBoolType;
		return null;
	}

	public Object visitAnd(And n) {
		super.visitAnd(n);
		matchTypesExact(n.left.type, theBoolType, n.left.pos);
		matchTypesExact(n.right.type, theBoolType, n.right.pos);
		n.type = theBoolType;
		return null;
	}

	public Object visitOr(Or n) {
		super.visitOr(n);
		matchTypesExact(n.left.type, theBoolType, n.left.pos);
		matchTypesExact(n.right.type, theBoolType, n.right.pos);
		n.type = theBoolType;
		return null;
	}

	public Object visitArrayLength(ArrayLength n) {
		super.visitArrayLength(n);
		if (n.exp.type == null || !(n.exp.type instanceof ArrayType)) return null;
		n.type = theIntType;
		return null;
	}

	public Object visitArrayLookup(ArrayLookup n) {
		super.visitArrayLookup(n);
		matchTypesExact(n.idxExp.type, theIntType, n.idxExp.pos);
		if (n.arrExp.type == null || !(n.arrExp.type instanceof ArrayType)) return null;
		n.type = n.arrExp.type;
		return null;
	}

	public Object visitInstVarAccess(InstVarAccess n) {
		super.visitInstVarAccess(n);
		if (n.exp == null) return null;
		n.varDec = instVarLookup(n.varName, n.type, n.pos, "Instance variable not defined");
		if (n.varDec != null) n.type = n.varDec.type;
		return null;
	}

	public Object visitCast(Cast n) {
		super.visitCast(n);
		matchTypesAssign(n.exp.type, n.castType, n.exp.pos);
		matchTypesAssign(n.castType, n.exp.type, n.castType.pos);
		n.type = n.castType;
		return null;
	}

	public Object visitInstanceOf(InstanceOf n) {
		super.visitInstanceOf(n);
		matchTypesAssign(n.exp.type, n.checkType, n.exp.pos);
		matchTypesAssign(n.checkType, n.exp.type, n.checkType.pos);
		n.type = theBoolType;
		return null;
	}

	public Object visitNewObject(NewObject n) {
		super.visitNewObject(n);
		n.type = n.objType;
		return null;
	}

	public Object visitNewArray(NewArray n) {
		super.visitNewArray(n);
		matchTypesExact(n.sizeExp.type, theIntType, n.sizeExp.pos);
		n.type = new ArrayType(n.pos, n.objType);
		return null;
	}

	public Object visitCall(Call n) {
		super.visitCall(n);
		if (n.obj == null) return null;
		n.methodLink = methodLookup(n.methName, n.type, n.pos, "Method not defined.");
		if (n.methodLink == null) return null;
		if (n.parms.size() != n.methodLink.formals.size()) return null;
		for (int i = 0; i < n.parms.size(); i++) {
			final Exp parmType = n.parms.get(i);
			matchTypesAssign(parmType.type, n.methodLink.formals.get(i).type, parmType.pos);
		}
		n.type = n.methodLink instanceof MethodDeclVoid ? theVoidType : ((MethodDeclNonVoid) n.methodLink).rtnType;
		return null;
	}

	public Object visitAssign(Assign n) {
		super.visitAssign(n);
		if (!(n.lhs instanceof IdentifierExp || n.lhs instanceof ArrayLookup || n.lhs instanceof InstVarAccess)) return null;
		matchTypesAssign(n.lhs.type, n.rhs.type, n.lhs.pos);
		return null;
	}

	public Object visitLocalVarDecl(LocalVarDecl n) {
		super.visitLocalVarDecl(n);
		matchTypesAssign(n.initExp.type, n.type, n.initExp.pos);
		return null;
	}

	public Object visitIf(If n) {
		super.visitIf(n);
		matchTypesExact(n.exp.type, theBoolType, n.exp.pos);
		return null;
	}

	public Object visitWhile(While n) {
		super.visitWhile(n);
		matchTypesExact(n.exp.type, theBoolType, n.exp.pos);
		return null;
	}

	public Object visitCase(Case n) {
		super.visitCase(n);
		matchTypesExact(n.exp.type, theIntType, n.exp.pos);
		return null;
	}

	public Object visitMethodDeclVoid(MethodDeclVoid n) {
		super.visitMethodDeclVoid(n);
		ClassDecl current = n.classDecl.superLink;
		while (current != null) {
			if (!current.methodTable.containsKey(n.name)) {
				current = current.superLink;
				continue;
			}
			final MethodDecl currentMethod = current.methodTable.get(n.name);
			if (!(currentMethod instanceof MethodDeclVoid)) return null;
			if (n.formals.size() != currentMethod.formals.size()) return null;
			for (int i = 0; i < n.formals.size(); i++) {
				final VarDecl parmType = n.formals.get(i);
				matchTypesAssign(parmType.type, currentMethod.formals.get(i).type, parmType.pos);
			}
			n.superMethod = currentMethod;
			break;
		}
		return null;
	}
}