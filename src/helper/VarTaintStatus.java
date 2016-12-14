package helper;

/*
 * Tells whether a variable at a particular line in the function in the program to be analyzed is tainted or not
 */

public class VarTaintStatus {
	private String var;
	private Integer lineNum;
	private Boolean isTainted;
	private Integer scopeNum;
	
	public VarTaintStatus(String var, Integer lineNum, Boolean isTainted, Integer scopeNum) {
		this.setVar(var);
		this.setLineNum(lineNum);
		this.setIsTainted(isTainted);
		this.setScopeNum(scopeNum);
	}

	public String getVar() {
		return var;
	}

	public void setVar(String var) {
		this.var = var;
	}

	public Integer getLineNum() {
		return lineNum;
	}

	public void setLineNum(Integer lineNum) {
		this.lineNum = lineNum;
	}

	public Boolean getIsTainted() {
		return isTainted;
	}

	public void setIsTainted(Boolean isTainted) {
		this.isTainted = isTainted;
	}

	public Integer getScopeNum() {
		return scopeNum;
	}

	public void setScopeNum(Integer scopeNum) {
		this.scopeNum = scopeNum;
	}
}
