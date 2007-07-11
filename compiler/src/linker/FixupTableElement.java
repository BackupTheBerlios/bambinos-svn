package linker;

public class FixupTableElement {

	public String module = new String();
	public String name = new String();
	public Integer offset = new Integer(0);

	public FixupTableElement(String module, Integer offset) {
		super();
		this.module = module;
		this.offset = offset;
	}

	public FixupTableElement() {
	}

}
