package nc.ms.tb.formula.script.core.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import nc.ms.tb.formula.script.Calculator;
import nc.ms.tb.formula.script.Function;
import nc.ms.tb.formula.script.NameSpace;
import nc.ms.tb.formula.script.core.ExcelAreaStack;
import nc.ms.tb.formula.script.core.ObjectArray;
import nc.vo.mdm.pub.NtbLogger;
import nc.vo.tb.formula.IFormulaDataCell;

public class FunctionCall extends AbstractNode {
	private String name;
	private List arguments;

	public FunctionCall() {
	}

	public void setName(String s) {
		if (s == null) {
			return;
		}
		this.name = s.trim().toUpperCase();
	}

	public void addArgument(Object conditionalexpression) {
		if (this.arguments == null)
			this.arguments = new ArrayList();
		this.arguments.add(conditionalexpression);
	}

	public Object eval(Calculator calculator) throws UtilEvalError {
		NameSpace namespace = calculator.getNameSpace();
		if (namespace == null)
			namespace = nc.ms.tb.formula.script.core.DefaultNameSpace.getInstance();
		Function function = namespace.getMethod(this.name, calculator);
		if (function == null) {
			String info = nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_rule", "01420rule-000034");
			info = info + "[" + this.name + "]";
			info = info + nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_rule", "01801rul_000315");
			NtbLogger.error(info);
			return 0;
		}

		function.setCalculator(calculator);
		Stack stack = new Stack();
		if (this.arguments != null) {
			int i = this.arguments.size();
			for (int j = i - 1; j >= 0; j--) {
				ExcelAreaStack paramStack = new ExcelAreaStack();
				Object obj = this.arguments.get(j);
				if ((obj instanceof Node)) {

					if (function.isCellValue()) {
						obj = ((Node) obj).eval(calculator);
					} else if (function.cellPlace() == -1) {
						calculator.setCellValueIsCell(true);
						obj = ((Node) obj).eval(calculator);
						calculator.setCellValueIsCell(false);
					} else if (j == function.cellPlace()) {
						calculator.setCellValueIsCell(true);
						obj = ((Node) obj).eval(calculator);
						calculator.setCellValueIsCell(false);
					} else {
						obj = ((Node) obj).eval(calculator);
					}

					if ((obj instanceof ObjectArray)) {
						ObjectArray oa = (ObjectArray) obj;
						stack.addAll(((ObjectArray) obj).getValueList());
						paramStack.addAll(((ObjectArray) obj).getValueList());
						paramStack.setColCount(oa.getColCount());
						paramStack.setRowCount(oa.getRowCount());
					} else {
						stack.push(obj);
						paramStack.push(obj);
					}
					if (function.isRecountParam()) {
						function.getParamStack().add(paramStack);
					}
				}
			}
		}
		return function.run(stack);
	}

	public void collect(Map map) {
		Object obj = (List) map.get(this.name);
		if (obj == null) {
			obj = new ArrayList();
			map.put(this.name, obj);
		}
		if (this.arguments != null) {
			ArrayList arraylist = new ArrayList();
			((List) obj).add(arraylist);
			int i = 0;
			for (int j = this.arguments.size(); i < j; i++) {
				arraylist.add(this.arguments.get(i).toString().replaceAll("[\"']", ""));
			}
		}
	}

	public String toString() {
		StringBuffer stringbuffer = new StringBuffer();
		stringbuffer.append(this.name).append("(");
		for (int i = 0; (this.arguments != null) && (i < this.arguments.size()); i++) {
			if (i != 0)
				stringbuffer.append(",");
			stringbuffer.append(this.arguments.get(i));
		}

		stringbuffer.append(")");
		return stringbuffer.toString();
	}

	@Override
	public String toValue(Calculator calculator) {
		try {
			if ((this.name.startsWith("UFIND")) || (this.name.startsWith("PREFIND")) || (this.name.startsWith("CELL_IUFO")) || (this.name.startsWith("FLEXEXPRESS")) || (this.name.startsWith("UFO"))) {

				return toString();
			}
			if ("FIND".equals(this.name)) {
				Object value = null;
				if (calculator.getContext().getWorkBook() != null) {
					int oldType = calculator.getContext().getCutCubeType();
					calculator.getContext().setCutCubeType(16);

					value = eval(calculator);
					calculator.getContext().setCutCubeType(oldType);
				} else {
					value = eval(calculator);
				}

				return formatValue(value);
			}
			Object value = eval(calculator);
			return formatValue(value);
		} catch (UtilEvalError e) {
			NtbLogger.error(e);
		}
		return "";
	}

	private String formatValue(Object value) {
		double rtn = 0.0D;
		if ((value instanceof java.util.Collection)) {
			for (Object obj : (java.util.Collection) value) {
				
				IFormulaDataCell cell = (IFormulaDataCell) obj;
				// 20172014 tsy 当cell.getDataCell().getCellValue().getValue()为null时会报错
				// if ((cell.getDataCell() != null) && (cell.getDataCell().getCellValue() != null)) {
				// rtn += cell.getDataCell().getCellValue().getValue().doubleValue();
				// }
				if ((cell.getDataCell() != null) && (cell.getDataCell().getCellValue() != null) && (cell.getDataCell().getCellValue().getValue() != null)) {
					rtn += cell.getDataCell().getCellValue().getValue().doubleValue();
				}

				// 20170214 end

			}

		} else if ((value instanceof Number)) {
			rtn = ((Number) value).doubleValue();

		} else if ((value instanceof IFormulaDataCell)) {
			IFormulaDataCell cell = (IFormulaDataCell) value;
			if ((cell.getDataCell() != null) && (cell.getDataCell().getCellValue() != null) && (cell.getDataCell().getCellValue().getValue() != null)) {
				rtn = cell.getDataCell().getCellValue().getValue().doubleValue();
			}

		} else {
			rtn = 0.0D;
		}

		java.text.DecimalFormat fommat = new java.text.DecimalFormat("##.########");
		return fommat.format(rtn);
	}

	@Override
	public String toDesc(DescriptionContext descContext) {
		NameSpace namespace = nc.ms.tb.formula.script.core.DefaultNameSpace.getInstance();
		Function function = namespace.getMethod(this.name, new Calculator());
		if (function == null)
			throw new InterpreterError("no function found: " + this.name);
		Stack stack = new Stack();
		if (this.arguments != null) {
			int i = this.arguments.size();
			for (int j = i - 1; j >= 0; j--) {
				Object obj = this.arguments.get(j);
				if ((obj instanceof Node)) {
					obj = ((AbstractNode) obj).toDesc(descContext);

					if ((obj instanceof String)) {
						String stringParam = (String) obj;
						if ((stringParam.startsWith("'")) && (stringParam.endsWith("'"))) {
							stack.push(stringParam.substring(1, stringParam.length() - 1));
						} else {
							stack.push(obj);
						}
					} else {
						stack.push(obj);
					}
				}
			}
		}

		return function.toDesc(stack, descContext);
	}

	@Override
	public List<Node> getNodes(List<Node> nodeList) {
		nodeList.add(this);
		return nodeList;
	}

	public String getName() {
		return this.name;
	}

	public List getArguments() {
		return this.arguments;
	}
}
