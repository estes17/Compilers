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

			IdentifierType parent = ((IdentifierType) src).link;

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
		return md.;
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
		if (n.exp.type == null || n.exp.type instanceof ArrayType) return null;
		n.type = theIntType;
		return null;
	}

	public Object 
}
