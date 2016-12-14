package helper;

/*
 * Helps in managing scope of variables 
 */

public class ScopeStack {
	private String item;
	private Integer lineNum;
	private Boolean isTainted;
	
	public ScopeStack(String item, Integer lineNum, Boolean isTainted) {
		this.item = item;
		this.lineNum = lineNum;
		this.setIsTainted(isTainted);
	}
	
	public String getItem() {
		return item;
	}
	
	public void setItem(String item) {
		this.item = item;
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
}