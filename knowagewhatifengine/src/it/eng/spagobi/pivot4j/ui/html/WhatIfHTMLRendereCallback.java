package it.eng.spagobi.pivot4j.ui.html;

import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.olap4j.Axis;
import org.olap4j.OlapException;
import org.olap4j.Position;
import org.olap4j.metadata.Dimension;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;
import org.pivot4j.sort.SortCriteria;
import org.pivot4j.transform.PlaceMembersOnAxes;
import org.pivot4j.ui.CellTypes;
import org.pivot4j.ui.command.DrillDownCommand;
import org.pivot4j.ui.command.UICommand;
import org.pivot4j.ui.html.HtmlRenderCallback;
import org.pivot4j.ui.table.TableRenderContext;
import org.pivot4j.util.RenderPropertyUtils;

import it.eng.spagobi.engines.whatif.crossnavigation.CrossNavigationManager;
import it.eng.spagobi.engines.whatif.crossnavigation.SpagoBICrossNavigationConfig;
import it.eng.spagobi.engines.whatif.crossnavigation.TargetClickable;
import it.eng.spagobi.engines.whatif.model.SpagoBICellWrapper;
import it.eng.spagobi.engines.whatif.model.SpagoBIPivotModel;
import it.eng.spagobi.utilities.engines.SpagoBIEngineRuntimeException;

public class WhatIfHTMLRendereCallback extends HtmlRenderCallback {
	private boolean showProperties = false;
	private HashMap<Member, Integer> memberPositions;
	private boolean measureOnRows;
	private Map<Integer, String> positionMeasureMap;
	private boolean initialized = false;

	public WhatIfHTMLRendereCallback(Writer writer) {
		super(writer);
		memberPositions = new HashMap<Member, Integer>();
		showProperties = true;
		setRowHeaderLevelPadding(20);
	}

	@Override
	protected Map<String, String> getCellAttributes(TableRenderContext context) {

		Map<String, String> attributes = super.getCellAttributes(context);
		// initializeInternal(context);
		if (context.getCellType() == CellTypes.VALUE) {

			initializeInternal(context);

			// need the name of the measure to check if it's editable
			String measureName = getMeasureName(context);
			attributes.put("contentEditable", "true");
			int colId = context.getColumnIndex();
			int rowId = context.getRowIndex();
			int positionId = context.getCell().getOrdinal();
			// String memberUniqueName = context.getMember().getUniqueName();
			String id = positionId + "!" + rowId + "!" + colId + "!" + System.currentTimeMillis() % 1000;
			attributes.put("ondblclick", "javascript:Sbi.olap.eventManager.makeEditable('" + id + "','" + measureName + "')");
			attributes.put("id", id);
		} else if (context.getCellType() == CellTypes.VALUE) {
			String uniqueName = context.getMember().getUniqueName();
			int axis = context.getAxis().axisOrdinal();
			attributes.put("ondblclick", "javascript:Sbi.olap.eventManager.setCalculatedFieldParent('" + uniqueName + "','" + axis + "')");

		}
		return attributes;
	}

	@Override
	public void renderCommands(TableRenderContext context, List<UICommand<?>> commands) {

		String drillMode = context.getRenderer().getDrillDownMode();
		if (!isEmptyNonPropertyCell(context)) {
			if (context.getCellType() == CellTypes.VALUE && !context.getCell().isEmpty()) {
				int ordinal = context.getCell().getOrdinal();
				if (context.getRenderer().getEnableDrillThrough()) {

					Map<String, String> attributes = new TreeMap<String, String>();
					attributes.put("src", "../img/ico_search.gif");
					attributes.put("id", "drillt");
					attributes.put("ng-click", "drillThrough(" + ordinal + ")");
					startElement("img", attributes);
					endElement("img");
				}
			}

			if (context.getMember() != null && context.getMember().getMemberType() != null
					&& !context.getMember().getMemberType().name().equalsIgnoreCase("Measure")) {
				Map<String, String> attributes = new TreeMap<String, String>();
				if (commands != null && !commands.isEmpty()) {
					for (UICommand<?> command : commands) {
						String cmd = command.getName();

						int colIdx = context.getColumnIndex();
						int rowIdx = context.getRowIndex();

						int axis = 0;
						if (context.getAxis() != null) {
							axis = context.getAxis().axisOrdinal();
						}
						int memb = 0;
						if (context.getPosition() != null) {
							memb = context.getAxis().axisOrdinal();
						}
						int pos = 0;
						if (context.getAxis() == Axis.COLUMNS) {
							pos = rowIdx;
						} else {
							pos = colIdx;
						}

						String uniqueName = context.getMember().getUniqueName();
						String positionUniqueName = context.getPosition().getMembers().toString();
						if (cmd != null) {
							if ((cmd.equalsIgnoreCase("collapsePosition") || cmd.equalsIgnoreCase("drillUp") || cmd.equalsIgnoreCase("collapseMember"))
									&& !drillMode.equals(DrillDownCommand.MODE_REPLACE)) {
								attributes.put("src", "../img/minus.gif");
								attributes.put("ng-click",
										"drillUp(" + axis + " , " + pos + " , " + memb + ",'" + uniqueName + "','" + positionUniqueName + " ')");
								startElement("img", attributes);
								endElement("img");

							} else if ((cmd.equalsIgnoreCase("expandPosition") || cmd.equalsIgnoreCase("drillDown") || cmd.equalsIgnoreCase("expandMember"))) {

								attributes.put("src", "../img/plus.gif");
								attributes.put("ng-click",
										"drillDown(" + axis + " , " + pos + " , " + memb + ",'" + uniqueName + "','" + positionUniqueName + "' )");

								startElement("img", attributes);
								endElement("img");
							} else {
								if (context.getAxis() == Axis.ROWS && !isPropertyCell(context)) {

									attributes.put("src", "../img/nodrill.png");
									attributes.put("style", "padding : 2px");
									startElement("img", attributes);
									endElement("img");
								}

							}
							if (cmd.equalsIgnoreCase("sort")) {
								setSortingCommand(context);

							}

						}
					}
				} else {
					if (context.getAxis() == Axis.ROWS && !isPropertyCell(context)) {

						attributes.put("src", "../img/nodrill.png");
						attributes.put("style", "padding : 2px");
						startElement("img", attributes);
						endElement("img");
					}
				}
			} else if (context.getMember() != null && context.getMember().getMemberType() != null
					&& context.getMember().getMemberType().name().equalsIgnoreCase("Measure")) {

				for (UICommand<?> command : commands) {
					String cmd = command.getName();
					if (cmd.equalsIgnoreCase("sort")) {
						setSortingCommand(context);

					}

				}
			}
		}

	}

	@Override
	public void renderContent(TableRenderContext context, String label, Double value) {

		// super.renderContent(context, label, value);
		String link = null;
		String propertyCategory = context.getRenderPropertyCategory();
		RenderPropertyUtils propertyUtils = getRenderPropertyUtils();
		link = propertyUtils.getString("link", propertyCategory, null);

		if (link == null) {
			Map<String, String> attributes = new TreeMap<String, String>();
			String drillMode = context.getRenderer().getDrillDownMode();
			if ((context.getCellType() == "title") && !label.equalsIgnoreCase("Measures")) {

				int colIdx = context.getColumnIndex();
				int rowIdx = context.getRowIndex();

				int axis = 0;
				if (context.getAxis() != null) {
					axis = context.getAxis().axisOrdinal();
				}
				int memb = 0;
				if (context.getPosition() != null) {
					memb = context.getPosition().getOrdinal();
				}
				int pos = 0;
				if (context.getAxis() == Axis.COLUMNS) {
					pos = rowIdx;
				} else {
					pos = colIdx;
				}

				if (drillMode.equals(DrillDownCommand.MODE_REPLACE) && !context.getRenderer().getShowParentMembers()) {
					Hierarchy h = context.getHierarchy();
					PlaceMembersOnAxes pm = context.getModel().getTransform(PlaceMembersOnAxes.class);

					List<Member> visibleMembers = pm.findVisibleMembers(h);
					int d = 0;
					for (Member m : visibleMembers) {
						Level l = m.getLevel();
						d = l.getDepth();
						if (d != 0) {
							break;
						}
					}

					// For drill replace the context.getPosition() and
					// context.getMember are empty.
					String uniqueName = "x";
					String positionUniqueName = "x";

					if (context != null) {
						if (context.getPosition() != null && context.getPosition() != null) {
							positionUniqueName = context.getPosition().getMembers().toString();
						}
						if (context.getMember() != null) {
							uniqueName = context.getMember().getUniqueName();
						}

					}

					if (d != 0) {
						attributes.put("src", "../img/arrow-up.png");
						attributes.put("onClick", "javascript:Sbi.olap.eventManager.drillUp(" + axis + " , " + pos + " , " + memb + ",'" + uniqueName + "','"
								+ positionUniqueName + "' )");
						startElement("img", attributes);
						endElement("img");
					}
					writeContent(label);

				} else if (!drillMode.equals(DrillDownCommand.MODE_REPLACE)) {
					writeContent(label);
				}
			} else {

				// start OSMOSIT cross nav button
				SpagoBIPivotModel sbiModel = (SpagoBIPivotModel) context.getModel();
				SpagoBICrossNavigationConfig crossNavigation = sbiModel.getCrossNavigation();
				if (crossNavigation != null && crossNavigation.isButtonClicked()
						&& !crossNavigation.getModelStatus().equalsIgnoreCase(new String("locked_by_other"))
						&& !crossNavigation.getModelStatus().equalsIgnoreCase(new String("locked_by_user")) && context.getCellType() == CellTypes.VALUE) {

					int colId = context.getColumnIndex();
					int rowId = context.getRowIndex();
					int positionId = context.getCell().getOrdinal();
					String id = positionId + "!" + rowId + "!" + colId + "!" + System.currentTimeMillis() % 1000;
					attributes.put("src", "../img/cross-navigation.gif");
					attributes.put("onload", "javascript:Sbi.olap.eventManager.createCrossNavigationMenu('" + id + "')");
					attributes.put("id", id);
					startElement("img", attributes);
					endElement("img");

					writeContent(label);
				} else {
					// TODO: OSMOSIT create member clickable
					List<TargetClickable> targetsClickable = sbiModel.getTargetsClickable();
					if (targetsClickable != null && targetsClickable.size() > 0) {
						Member member = context.getMember();
						if (member != null) {
							String url = CrossNavigationManager.buildClickableUrl(member, targetsClickable);
							if (url != null) {
								attributes.remove("onClick");
								attributes.remove("src");
								attributes.put("href", url);
								startElement("a", attributes);
								writeContent(label);
								endElement("a");
							} else {
								writeContent(label);
							}
						} else {

							writeContent(label);
						}
					} else {

						writeContent(label);
					}

				}

			}
		} else {
			Map<String, String> attributes = new HashMap<String, String>(1);
			attributes.put("href", link);

			startElement("a", attributes);
			writeContent(label);
			endElement("a");
		}

	}

	private boolean isPropertyCell(TableRenderContext context) {

		if (showProperties && context.getLevel() != null && isEmptyNonPropertyCell(context) && context.getRenderer().getPropertyCollector() != null) {
			List<org.olap4j.metadata.Property> propertieds = context.getRenderer().getPropertyCollector().getProperties(context.getLevel());
			return (propertieds != null && propertieds.size() > 0);
		}
		return false;
	}

	private boolean isEmptyNonPropertyCell(TableRenderContext context) {

		Member member = context.getMember();

		if (member != null && showProperties) {
			Integer memberPosition = null;

			if (context.getAxis().axisOrdinal() == (Axis.ROWS.axisOrdinal())) {
				memberPosition = context.getColumnIndex();

			} else {
				memberPosition = context.getRowIndex();
			}
			if (!memberPositions.containsKey(member)) {
				memberPositions.put(member, memberPosition);

			}

			Integer previousPositions = memberPositions.get(member);

			if (previousPositions == memberPosition) {
				return false;
			}
			return true;
		}

		return false;
	}

	private String getMeasureName(TableRenderContext context) { // (RenderContext
		// context)
		int coordinate;
		if (this.measureOnRows) {
			coordinate = context.getRowIndex(); // coordinate =
			// context.getRowIndex();
		} else {
			coordinate = context.getColumnIndex(); // context.getColumnIndex()
		}
		String measureName = this.positionMeasureMap.get(coordinate);

		if (measureName == null) {
			measureName = ((SpagoBICellWrapper) context.getCell()).getMeasureName();
			this.positionMeasureMap.put(coordinate, measureName);
		}

		return measureName;

	}

	private void initializeInternal(TableRenderContext context) {
		if (!this.initialized) {
			this.measureOnRows = true;
			this.initialized = true;
			this.positionMeasureMap = new HashMap<Integer, String>();

			// check if the measures are in the rows or in the columns
			List<Member> columnMembers = null;
			Position p = context.getColumnPosition();
			if (p != null) {
				columnMembers = p.getMembers();
			}

			try {
				if (columnMembers != null) {
					for (int i = 0; i < columnMembers.size(); i++) {
						Member member = columnMembers.get(i);
						if (member.getDimension().getDimensionType().equals(Dimension.Type.MEASURE)) {
							this.measureOnRows = false;
						}
					}
				}
			} catch (OlapException e) {
				throw new SpagoBIEngineRuntimeException("Erro getting the measure of a rendered cell ", e);
			}
		}
	}

	private void setSortingCommand(TableRenderContext context) {
		int axis = 0;
		if (context.getAxis() != null) {
			axis = context.getAxis().axisOrdinal();
		}

		int axisToSort = 0;
		if (axis == Axis.ROWS.axisOrdinal()) {
			axisToSort = Axis.COLUMNS.axisOrdinal();
		} else {
			axisToSort = Axis.ROWS.axisOrdinal();
		}

		Map<String, String> attributes = new TreeMap<String, String>();

		if (context.getRenderer().getEnableSort()) {
			if (context.getModel().isSorting(context.getPosition()) && context.getModel().getSortCriteria() != null) {
				if (context.getModel().getSortCriteria().equals(SortCriteria.ASC) || context.getModel().getSortCriteria().equals(SortCriteria.BASC)
						|| context.getModel().getSortCriteria().equals(SortCriteria.TOPCOUNT)) {
					if (axisToSort == Axis.ROWS.axisOrdinal()) {
						attributes.put("src", "../img/ASC-rows.png");
					} else {
						attributes.put("src", "../img/ASC-columns.png");
					}

					attributes.put("ng-click", "sort(" + axisToSort + " , " + axis + " , '" + context.getPosition().getMembers().toString() + "' )");
					System.out.println(context.getMember() + " has sorting " + context.getModel().getSortCriteria());
					startElement("img", attributes);
					endElement("img");
				} else if (context.getModel().getSortCriteria().equals(SortCriteria.DESC) || context.getModel().getSortCriteria().equals(SortCriteria.BDESC)
						|| context.getModel().getSortCriteria().equals(SortCriteria.BOTTOMCOUNT)) {

					if (axisToSort == Axis.ROWS.axisOrdinal()) {
						attributes.put("src", "../img/DESC-rows.png");
					} else {
						attributes.put("src", "../img/DESC-columns.png");
					}

					attributes.put("ng-click", "sort(" + axisToSort + " , " + axis + " , '" + context.getPosition().getMembers().toString() + "' )");
					System.out.println(context.getMember() + " has sorting " + context.getModel().getSortCriteria());
					startElement("img", attributes);
					endElement("img");
				}
			} else {
				context.getModel().setSorting(false);
				if (axisToSort == Axis.ROWS.axisOrdinal()) {
					attributes.put("src", "../img/noSortRows.png");
				} else {
					attributes.put("src", "../img/noSortColumns.png");
				}

				attributes.put("ng-click", "sort(" + axisToSort + " , " + axis + " , '" + context.getPosition().getMembers().toString() + "' )");

				startElement("img", attributes);
				endElement("img");
			}

		}

	}
}
