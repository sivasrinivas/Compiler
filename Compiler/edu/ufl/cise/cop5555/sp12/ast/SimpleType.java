package edu.ufl.cise.cop5555.sp12.ast;

import edu.ufl.cise.cop5555.sp12.Kind;

public class SimpleType extends Type {
	
	public Kind type;
	
	public SimpleType(Kind type){
		this.type = type;
	}

	@Override
	public Object visit(ASTVisitor v, Object arg) throws Exception {
		return v.visitSimpleType(this,arg);
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof SimpleType){
			SimpleType st=(SimpleType) obj;
			if(this.type==(st.type))
				return true;
		}
		return false;
	}

}
