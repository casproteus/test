package org.cas.client.platform.bar.dialog;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.cas.client.platform.bar.BarUtil;
import org.cas.client.platform.bar.action.Cmd_ChangePrice;
import org.cas.client.platform.bar.action.Cmd_DiscItem;
import org.cas.client.platform.bar.action.Cmd_SplitItem;
import org.cas.client.platform.bar.dialog.modifyDish.AddModificationDialog;
import org.cas.client.platform.bar.i18n.BarDlgConst;
import org.cas.client.platform.bar.model.DBConsts;
import org.cas.client.platform.bar.model.Dish;
import org.cas.client.platform.bar.model.Rule;
import org.cas.client.platform.bar.print.PrintService;
import org.cas.client.platform.bar.uibeans.ArrowButton;
import org.cas.client.platform.cascontrol.dialog.logindlg.LoginDlg;
import org.cas.client.platform.cascustomize.CustOpts;
import org.cas.client.platform.casutil.ErrorUtil;
import org.cas.client.platform.casutil.L;
import org.cas.client.platform.casutil.PIMPool;
import org.cas.client.platform.pimmodel.PIMDBModel;
import org.cas.client.platform.pimview.pimscrollpane.PIMScrollPane;
import org.cas.client.platform.pimview.pimtable.DefaultPIMTableCellRenderer;
import org.cas.client.platform.pimview.pimtable.PIMTable;
import org.cas.client.platform.pimview.pimtable.PIMTableColumn;
import org.cas.client.platform.pimview.pimtable.PIMTableRenderAgent;
import org.cas.client.resource.international.DlgConst;
import org.cas.client.resource.international.PaneConsts;

public class BillPanel extends JPanel implements ActionListener, ComponentListener, PIMTableRenderAgent, ListSelectionListener, MouseMotionListener, MouseListener{
	
	SalesPanel salesPanel;
	BillListPanel billListPanel;
	public JToggleButton billButton;
	
	private int stepCounter;
	private int minStepCounter = CustOpts.custOps.getValue("minMoveStep") == null ? 5 : Integer.valueOf((String)CustOpts.custOps.getValue("minMoveStep"));

    private boolean isDragging;
    public ArrayList<Dish> orderedDishAry = new ArrayList<Dish>();
    
    //Bill property (not for specific item).the info should be retrieved from bill record if have.
    private int billID;
    public float discount;
    public float totalGst;
	public float totalQst;
	public float subTotal;
	public int tip;
	public int serviceFee;
    
    int received;
    int cashback;
    public String comment = "";
    public int status = DBConsts.original;
    
	public BillPanel(SalesPanel salesPanel) {
		this.salesPanel = salesPanel;
		initComponent();
	}

	public BillPanel(BillListPanel billListPanel, JToggleButton billButton) {
		this.billListPanel = billListPanel;
		this.billButton = billButton;
		initComponent();
	}
	
	public void createAndPrintNewOutput() {
		//if there's any new bill, send it to kitchen first, and this also made the output generated.
		List<Dish> dishes = getNewDishes();
		if (dishes != null && dishes.size() > 0) {
			sendNewOrdersToKitchenAndDB(dishes);
		}else {
			sendNewOrdersToKitchenAndDB(BarUtil.generateAnEmptyDish());
 		}
	}
	
	//if there's new dish added.... update the total value field of bill record.
	//and make sure new added dish will be updated with new information.
	public void billPricesUpdateToDB() {
		BillPanel.updateBillRecordPrices(this);		//in case if added service fee or discout of bill. ??? so this means, when added discount, will not save to db immediatly?
		initContent();	//always need to initContent, to make sure dish in selection ary has new property. e.g. saved dish should has different color.,
	}
		
	public void printBill(String tableID, String billIndex, String opentime, boolean isToCustomer) {
		
        if(status >= DBConsts.completed || status < DBConsts.original || comment.contains(PrintService.OLD_GST)) { //for completed or reopened completed bill
        	PrintService.exePrintInvoice(this, false, isToCustomer, true);
        }else {		//for original or billprinted or reopeneed billPrinted bills go here..
	        PrintService.exePrintBill(this, orderedDishAry, false);
	        status = DBConsts.billPrinted;
	        comment += PrintService.REF_TO + getBillID();
	        comment = replaceMoney(comment, lblSubTotle.getText());
			//update the total price of the target bill, 
			//---because when add dish into the billPane, bill in db will not get updated.
			StringBuilder sql = new StringBuilder("update bill set total = ")
					.append(Math.round(Float.valueOf(valTotlePrice.getText()) * 100))
					.append(", discount = ").append(discount)
					.append(", serviceFee = ").append(serviceFee)
					.append(", status = ").append(DBConsts.billPrinted)//so the invoice can be printed with "save paper mode".
					.append(", comment = ' ").append(comment).append("'")
					.append(" where tableID = '").append(tableID).append("'")
					.append(" and BillIndex = '").append(billIndex).append("'")
					.append(" and openTime = '").append(opentime).append("'")
					.append(" and (status is null or status = ").append(DBConsts.original).append(")");
			try {
				PIMDBModel.getStatement().executeUpdate(sql.toString());
			}catch(Exception e) {
				L.e("BillPane", "Excepioint in print bill:" + sql, e);
			}
        }
	}

	private String replaceMoney(String comment, String subTotalStr) {
		//clean the subTotal string. incase it't like "subtotal:0.00"
		int p = subTotalStr.indexOf(":");
		if(p > 0) {
			subTotalStr = subTotalStr.substring(p + 1).trim();
		}
		p = subTotalStr.indexOf(BarOption.getMoneySign());	//in case there's dollar sign like "subtotal:$0.00"
		if(p >= 0) {
			subTotalStr = subTotalStr.substring(p + BarOption.getMoneySign().length());
		}
		
		//change the comment
		p = comment.indexOf(PrintService.OLD_SUBTOTAL);
		if(p > 0) {
			p += PrintService.OLD_SUBTOTAL.length();
			String firstPart = comment.substring(0, p);
			String secondPart = comment.substring(p);
			p = secondPart.indexOf("*");
			secondPart = p >0 ? subTotalStr + "\n" + secondPart.substring(p) : subTotalStr;
			comment = firstPart + secondPart;
		}
		return comment;
	}

	public int cloneCurrentBillRecord(String tableID, String billIndex, String opentime, int total) {
		if(total < 0) {
			total = Math.round(Float.valueOf(valTotlePrice.getText()) * 100);
		}
		
		//get other field out from db:
		StringBuilder sql = new StringBuilder("select * from bill where id = " + getBillID());
    	try {
    		ResultSet rs = PIMDBModel.getReadOnlyStatement().executeQuery(sql.toString());
            rs.next();
        	int cashReceived = rs.getInt("cashReceived");
        	int debitReceived = rs.getInt("debitReceived");
            int visaReceived = rs.getInt("visaReceived");
            int masterReceived = rs.getInt("masterReceived");
            int otherreceived = rs.getInt("otherreceived");
        	int cashBack = rs.getInt("cashback");
            int tip = rs.getInt("tip");
        
			//generate a bill in db and update the output with the new bill id
			String createtime = BarOption.df.format(new Date());
			sql = new StringBuilder(
	            "INSERT INTO bill(createtime, tableID, BillIndex, total, discount, tip, serviceFee, cashback, EMPLOYEEID, Comment,")
	            .append(" opentime, cashReceived, debitReceived, visaReceived, masterReceived, otherreceived) VALUES ('")
				.append(createtime).append("', '")
	            .append(tableID).append("', '")	//table
	            .append(billIndex).append("', ")			//bill
	            .append(total).append(", ")//Math.round(Float.valueOf(valTotlePrice.getText()) * 100)/num).append(", ")	//total
	            .append(discount).append(", ")
	            .append(tip).append(", ")
	            .append(serviceFee).append(", ")			//currently used for storing service fee -_-!
	            .append(cashBack).append(", ")	//discount
	            .append(LoginDlg.USERID).append(", '")		//emoployid
	            .append(comment).append("', '")
	            .append(opentime).append("', ")
	            .append(cashReceived).append(", ")
	            .append(debitReceived).append(", ")
	            .append(visaReceived).append(", ")
	            .append(masterReceived).append(", ")
	            .append(otherreceived).append(")");				//content
		
			PIMDBModel.getStatement().executeUpdate(sql.toString());
			
		   	sql = new StringBuilder("Select id from bill where createtime = '").append(createtime)
		   			.append("' and billIndex = '").append(billIndex).append("'");
            rs = PIMDBModel.getReadOnlyStatement().executeQuery(sql.toString());
            rs.beforeFirst();
            rs.next();
            return rs.getInt("id");
		 }catch(Exception e) {
			ErrorUtil.write(e);
			return -1;
		 }
	}

	void sendDishToKitchen(Dish dish, boolean isCancelled) {
		List<Dish> dishes = new ArrayList<Dish>();
		dishes.add(dish);
		sendDishesToKitchen(dishes, isCancelled);
	}
	
	//send to printer
	public void sendDishesToKitchen(List<Dish> dishes, boolean isCancelled) {
		//prepare the printing String and do printing
		String curTable = BarFrame.instance.cmbCurTable.getSelectedItem().toString();
		String curCustomerIdx = BarFrame.instance.getOnSrcCurBillIdx();
		String waiterName = BarFrame.instance.valOperator.getText();
		PrintService.exePrintOrderList(dishes, curTable, curCustomerIdx, waiterName, isCancelled);
	}
	
	//save to db output
	void persistDishesToOutput(List<Dish> dishes) {
		try {
		    for (Dish dish : dishes) {
		    	String curBillIndex = BarFrame.instance.getCurBillIndex();
		    	Dish.createOutput(dish, curBillIndex);	//at this moment, the num should have not been soplitted.
		        //in case some store need to stay in the interface after clicking the send button. 
                StringBuilder sql = new StringBuilder("Select id from output where SUBJECT = '")
                    .append(BarFrame.instance.cmbCurTable.getSelectedItem().toString()).append("' and CONTACTID = ")
                    .append(curBillIndex).append(" and PRODUCTID = ")
                    .append(dish.getId()).append(" and AMOUNT = ")
                    .append(dish.getNum()).append(" and TOLTALPRICE = ")
                    .append(dish.getTotalPrice()).append(" and DISCOUNT = ")
                    .append(dish.getDiscount()).append(" and EMPLOYEEID = ")
                    .append(LoginDlg.USERID).append(" and TIME = '")
                    .append(dish.getOpenTime()).append("'");
                ResultSet rs = PIMDBModel.getReadOnlyStatement().executeQuery(sql.toString());
                rs.beforeFirst();
                while (rs.next()) {
                	dish.setOutputID(rs.getInt("id"));
                }
                
                rs.close();
		    }
		}catch(Exception exp) {
			JOptionPane.showMessageDialog(this, DlgConst.FORMATERROR);
		    exp.printStackTrace();
		}
	}
	
	public List<Dish> getNewDishes() {
		List<Dish> newDishes = new ArrayList<Dish>();
		for (Dish dish : orderedDishAry) {
			if(dish.getOutputID() > -1)	//if it's already saved into db, ignore.
				continue;
			else {
				newDishes.add(dish);
			}
		}
		return newDishes;
	}
	
    @Override
	public void actionPerformed(ActionEvent e) {

        Object o = e.getSource();
		if(o instanceof ArrowButton) {
        	if(!checkStatus()) {
        		return;
        	}
    		int selectedRow =  table.getSelectedRow();
    		Dish dish = orderedDishAry.get(selectedRow);
	        if(o == btnMore) {
				if(dish.getOutputID() >= 0) {	//already saved
					BarFrame.setStatusMes(BarFrame.consts.SendItemCanNotModify());
					addContentToList(orderedDishAry.get(selectedRow));
				}else {			//not saved yet  //NOTE:getNum() couldn't be bigger than 10000, if it's saved, + button will insert a new line.
					int tQTY = orderedDishAry.get(selectedRow).getNum() + 1;
					dish.setNum(tQTY);
					String modify = BillListPanel.curDish.getModification();
					float priceInLabel = modify == null ? 0.0f : BarUtil.calculateLabelsPrices(modify.split(BarDlgConst.delimiter));
					dish.setTotalPrice((dish.getPrice() - dish.getDiscount() + Math.round(priceInLabel * 100)) * tQTY);
					
					table.setValueAt(tQTY % BarOption.MaxQTY + "x", selectedRow, 0);
					table.setValueAt(BarOption.getMoneySign() 
							+ BarUtil.formatMoney(dish.getTotalPrice()/100f),
							selectedRow, 3);
				}
	        } else if (o == btnLess) {
				if(dish.getOutputID() >= 0) {	//if it's already send, then do the removeItem.
					salesPanel.removeItem();
				}else {
					if(orderedDishAry.get(selectedRow).getNum() == 1) {
						if("true".equals(CustOpts.custOps.getValue("noticeForLastOne"))) {
							if (JOptionPane.showConfirmDialog(this, BarFrame.consts.COMFIRMDELETEACTION2(), DlgConst.DlgTitle,
				                    JOptionPane.YES_NO_OPTION) != 0) {
								table.setSelectedRow(-1);
								return;
							}
						}
						removeFromSelection(selectedRow);
					} else {
						int tQTY = orderedDishAry.get(selectedRow).getNum() - 1;
						dish.setNum(tQTY);
						String modify = BillListPanel.curDish.getModification();
						float priceInLabel = modify == null ? 0.0f : BarUtil.calculateLabelsPrices(modify.split(BarDlgConst.delimiter));
						dish.setTotalPrice((dish.getPrice() - dish.getDiscount() + Math.round(priceInLabel * 100)) * tQTY);
						table.setValueAt(tQTY == 1 ? "" : tQTY + "x"  , selectedRow, 0);		
						table.setValueAt(BarOption.getMoneySign()
								+ BarUtil.formatMoney(dish.getTotalPrice()/100f),
								selectedRow, 3);
					}
				}
	        }

			updateTotalArea();
        }else if(o == billButton){		//when bill button on top are clicked.
        	if(billListPanel != null && Cmd_SplitItem.getInstance().getSourceBtn().isSelected()) {
        		billButton.setSelected(!billButton.isSelected());
        		return;
        	}
        	
    		BarFrame.instance.setCurBillIdx(((JToggleButton)o).getText());
            BarFrame.instance.switchMode(2);
		}
		
	}

	@Override
	public void componentResized(ComponentEvent e) {
        reLayout();
	}

	@Override
	public void componentMoved(ComponentEvent e) {}

	@Override
	public void componentShown(ComponentEvent e) {}

	@Override
	public void componentHidden(ComponentEvent e) {}
	
//	@deprecated
	//was used when click send button, and found isFastFoodMode==true, then instead of returning back to table view, stay in
	//the sales view. now I decide to use unified processor : initContent().
//    void resetTableArea() {
//    	resetStatus();
//        Object[][] tValues = new Object[0][tblSelectedDish.getColumnCount()];
//        tblSelectedDish.setDataVector(tValues, header);
//        resetColWidth(scrContent.getWidth());
//        updateTotleArea();
//    }
    
    //table selection listener---------------------
	@Override
	public void valueChanged(ListSelectionEvent e) {
		//adjust more and less button status.
		int selectedRow =  table.getSelectedRow();
		if(selectedRow < 0 || orderedDishAry.size() < selectedRow + 1)
			return;
		btnMore.setEnabled(selectedRow >= 0 && selectedRow <= orderedDishAry.size());
		btnLess.setEnabled(selectedRow >= 0 && selectedRow <= orderedDishAry.size());
		
		//if no row selected, reset curdish and return.
		if(!btnMore.isEnabled()) {	//some time the selectedRow can be -1.
			BillListPanel.curDish = null;
			return;
		}

		//if in salesPanel mode, then adjust it's curDish and numberDlg if it's on show.
		//if in billListPanel mode, then change bill selection status and do moving dish.
		Dish selectedDish = orderedDishAry.get(selectedRow);
		if(salesPanel != null) {
			BillListPanel.curDish = selectedDish;
			if( BarFrame.numberPanelDlg.isVisible()) {	//if qty button seleted.
				Object obj = table.getValueAt(selectedRow,0);
				//update the qty in qtyDlg.
				if(obj != null)
					BarFrame.numberPanelDlg.setContents(obj.toString());
			}
			if(Cmd_DiscItem.getInstance().getSourceBtn().isSelected()) {
				Object obj = table.getValueAt(selectedRow,2);
				//update the discount in qtyDlg.
				if(obj != null)
					BarFrame.numberPanelDlg.setContents(obj.toString());
			}
		}else if(billListPanel != null) {
			if(Cmd_SplitItem.getInstance().getSourceBtn().isSelected()) {	//if in splite item mode, then do nothing but select the bill button.
				billButton.setSelected(!billButton.isSelected());
				return;
			}
			//only not in splitting can reach here. 
 			if(BillListPanel.curDish != null && billListPanel.getCurBillPanel() != null && billListPanel.getCurBillPanel() != this) {
				billListPanel.moveDishToBill(this);
				BillListPanel.curDish = null;
			}else {
				billButton.setSelected(true);
				BillListPanel.curDish = selectedDish;
			}
		}
	}

	//table row color----------------------------------------------------------
	@Override
	public Color getBackgroundAtRow(int row) {
		return null;
	}

	@Override
	public Color getForegroundAtRow(int row) {
		if(row < orderedDishAry.size()) {
			Dish dish = orderedDishAry.get(row);
			if(dish.getOutputID() > -1) {
				return Color.GRAY;
			}else {
				return Color.BLACK;
			}
		}else
			return null;
	}

	@Override
	//this method is used for deleting an item in the billPanel by a drag action.
	public void mouseReleased(MouseEvent e) {
		stepCounter = 0;
		if(isDragging == true) {
			isDragging = false;
			ListSelectionModel selectionModel = ((PIMTable)e.getSource()).getSelectionModel();
			int selectedRow = selectionModel.getMinSelectionIndex();
			if(selectedRow < 0 || selectedRow >= orderedDishAry.size()) 
				return;
			
			if(salesPanel != null && !BarFrame.numberPanelDlg.isVisible()) {	//if qty button not seleted.
				if(orderedDishAry.get(selectedRow).getOutputID() >= 0) {
					if (JOptionPane.showConfirmDialog(BarFrame.instance, BarFrame.consts.COMFIRMDELETEACTION(), DlgConst.DlgTitle,
		                    JOptionPane.YES_NO_OPTION) != 0) {// 确定删除吗？
						table.setSelectedRow(-1);
						return;
					}
				}

	        	if(!checkStatus()) {	//check if it's bill printed, if yes, need to reopen a bill.
            		return;
            	}
	        	
				removeFromSelection(selectedRow);
			}
		}
	}
	
	@Override
	public void mousePressed(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		if(e.getSource() == scrContent.getViewport()) {
			if(billListPanel != null) {
				if(Cmd_SplitItem.getInstance().getSourceBtn().isSelected()) {	//if in splite item mode, then do nothing but select the bill button.
					billButton.setSelected(!billButton.isSelected());
					return;
				}
				//if not in split item mode (split item button not pressed.)
				if(billListPanel.getCurBillPanel() == null) {
					billListPanel.getSelectedBillPannels().add(this);
					billButton.setSelected(!billButton.isSelected());
					return;
				}
	 			if(BillListPanel.curDish != null && billListPanel.getCurBillPanel() != this) {	//this there's already an item ready for move.
					billListPanel.moveDishToBill(this);
					BillListPanel.curDish = null;
				}else {	//no current item ready for split, then just select the. 
					billButton.setSelected(!billButton.isSelected());
					if(billButton.isSelected()) {
						BarFrame.instance.setCurBillIdx(billButton.getText());
						BarFrame.instance.setCurBillID(getBillID());
					}else {
						BillPanel panel = billListPanel.getCurBillPanel();
						if(panel != null) {
							BarFrame.instance.setCurBillIdx(panel.billButton.getText());
							BarFrame.instance.setCurBillID(panel.getBillID());
						}else {
							BarFrame.instance.setCurBillIdx("");
							BarFrame.instance.setCurBillID(0);
						}
					}
				}
			}
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		stepCounter++;
		if(stepCounter > minStepCounter ) {
			stepCounter = 0;
			isDragging = true;
		}
	}
	
	@Override
	public void mouseMoved(MouseEvent e) {}
	
    //add dish into the billPanel, tiggered by "+"button or the bubttons on menuPanel.
    void addContentToList(Dish dish) {
        int tRowCount = table.getRowCount(); // add content to the table.
        int tColCount = table.getColumnCount();
        int tValidRowCount = getUsedRowCount(); // get the used RowCount
        if (tRowCount == tValidRowCount) { // no line is empty, add a new Line.
            Object[][] tValues = new Object[tRowCount + 1][tColCount];
            for (int r = 0; r < tRowCount; r++)
                for (int c = 0; c < tColCount; c++)
                    tValues[r][c] = table.getValueAt(r, c);
            table.setDataVector(tValues, header);
            resetColWidth(scrContent.getWidth());
            if(BarFrame.secondScreen != null) {
            	BarFrame.customerFrame.billPanel.resetColWidth(scrContent.getWidth());
            }
        }else {
        	tRowCount--;
        }
        
        Dish newDish = dish.clone();		//@NOTE: incase the cloned dish contains outpurID properties.
        newDish.setOutputID(-1);
        newDish.setNum(1);
        int price = dish.getPrice();
        
        if("true".equals(dish.getPrompPrice()) && BarOption.isTreatPricePromtAsTaxInclude()) {
        	price = (int)Math.round(price / ((100 + BarOption.getGST() + BarOption.getQST()) / 100.0));
        }
        newDish.setTotalPrice(price * 1);
        newDish.setOpenTime(BarFrame.instance.valStartTime.getText());
        newDish.setBillIndex(BarFrame.instance.getCurBillIndex());
        newDish.setBillID(getBillID());
        orderedDishAry.add(newDish);				//valueChanged process. not being cleared immediately-----while now dosn't matter
        BillListPanel.curDish = newDish;
        
        //update the interface.
        table.setValueAt("", tValidRowCount, 0); // set the count.
        table.setValueAt(dish.getLanguage(CustOpts.custOps.getUserLang()), tValidRowCount, 1);// set the Name.
        table.setValueAt(dish.getSize() > 1 ? dish.getSize() : "", tValidRowCount, 2); // set the count.
        table.setValueAt(BarOption.getMoneySign() + BarUtil.formatMoney(price/100f), tValidRowCount, 3); // set the price.
        
        updateTotalArea();								//because value change will not be used to remove the record.
        SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
		        table.setSelectedRow(orderedDishAry.size() - 1);
		        if("true".equals(newDish.getPrompMofify())) {
		    		AddModificationDialog.getInstance().isSettingMode = false;
		    		AddModificationDialog.getInstance().initContent(BillListPanel.curDish.getModification(), 0);
		    		AddModificationDialog.getInstance().setVisible(true);
		        }
	        	//if the original is not 0.00, then will still be treated as price promp not a taxInclude.
		        if(newDish.getPrice() == 0 || "true".equals(newDish.getPrompPrice()) && !BarOption.isTreatPricePromtAsTaxInclude()) {
		        	Cmd_ChangePrice.getInstance().showPriceChangeDlg();
		        }
		        //by now price is changed, then should check the rule.
		        checkWithRules();
			}
		});
    }
    
    private void checkWithRules() {
    	Rule[] rules = BarFrame.menuPanel.getRules();
    	if(rules.length < 1) {
    		return;
    	}
    	
    	//reset the ruleId in the dishes and reset the discount value.
		removeTheRuleEffect();	
		
		// back up the rules and selected dishes.
		ArrayList<Rule> copyOfRules = new ArrayList<>();
		for (Rule rule : rules) {
			copyOfRules.add(rule);
		}
		ArrayList<Dish> copyOfSelection = new ArrayList<Dish>();
		for (Dish dish : orderedDishAry) {
			copyOfSelection.add(dish);
		}
		
		while(true) {
			if(copyOfRules.size() == 0) {
				break;
			}
			Rule rule = copyOfRules.get(0);
			if(ruleSatisfied(rule, copyOfSelection)) {	//all contained in current selection.
				discount += rule.getActionPrice();			// add the action of the rule into the discount.
			}else {
				copyOfRules.remove(rule);				// when a rule not satisfied, the rule will be removed from list.
			}
		}
		updateTotalArea();
	}

	//reset the ruleId in the dishes and reset the discount value.
	private void removeTheRuleEffect() {
		List<Integer> ruleIds = new ArrayList<Integer>();
		for(Dish dish : orderedDishAry) {
			Integer ruleId = dish.getRuleMark();
			if(ruleId != null) {	//care only not-null case, clean it, and if it not added ruleIds, add into the list.
				if(!ruleIds.contains(ruleId)) {
					ruleIds.add(ruleId);
				}
				dish.setRuleMark(null);
			}
		}
		for(Rule rule: BarFrame.menuPanel.getRules()) {
			if(ruleIds.contains(rule.getId())) {
				discount -= rule.getActionPrice();
			}
		}
	}

	//if all the dishes in the rule is contained in selectionList, then return the dishlist, so they can be removed).
	private boolean ruleSatisfied(Rule rule, ArrayList<Dish> copyOfSelection) {
		String[] dishIdsInRule = rule.getContent().split(","); 
		List<Dish> matchedDishesByRule = new ArrayList<Dish>();
		for(String id : dishIdsInRule) {
			for(int i = copyOfSelection.size() - 1; i >= 0; i--) {
				Dish dish = copyOfSelection.get(i);
				if(id.equals(String.valueOf(dish.getId()))) {
					matchedDishesByRule.add(dish);
					copyOfSelection.remove(dish);
					break;
				}
			}
		}
		//be vericareful to the size, because if there's a "," in the end, then the size is not acurate.
		if(matchedDishesByRule.size() == dishIdsInRule.length) {
			//mark those dished with a flag indication the rule id. so when it's removed from the selection, all relevent dishs will be removed the flag, and a action indicated price will
			//be added back. then a checkWithRules will be performed.
			for(Dish dish : matchedDishesByRule) {
				dish.setRuleMark(rule.getId());
			}
			return true;
		}else {
			for(Dish dish: matchedDishesByRule) {
				copyOfSelection.add(dish);
			}
			return false;
		}
	}
	
	public void sendNewOrdersToKitchenAndDB(List<Dish> dishes) {
		//if all record are new, means it's adding a new bill.otherwise, it's adding output to exixting bill.
//		if(dishes.size() == orderedDishAry.size()) {	//didn't set the idx when bill created, because don't wanto display idx if there's only 1 bill.
//		    BarFrame.instance.valCurBillIdx.setText(String.valueOf(BillListPanel.getANewBillIdx()));
//		}
		sendDishesToKitchen(dishes, false);
		persistDishesToOutput(dishes);
		table.repaint();//to update the color of dishes, it's saved, so it's not red anymore.
	}

    public void updateTotalArea() {
    	float gstRate = BarOption.getGST();
    	float qstRate = BarOption.getQST();
    	totalGst = 0;
    	totalQst = 0;
    	subTotal = 0;
    	
    	for(Dish dish: orderedDishAry) {
    		//get out the num.
    		int num = dish.getNum();
    		int pK = num /(BarOption.MaxQTY * 100);
    		if(num > BarOption.MaxQTY * 100) {
    			num = num %(BarOption.MaxQTY * 100);
    		}
    		int pS = num /BarOption.MaxQTY;
    		if(num > BarOption.MaxQTY) {
    			num = num % BarOption.MaxQTY;
    		}
    		
    		int totalPrice = dish.getTotalPrice();
    		float gst = totalPrice * (dish.getGst() * gstRate / 100f);	//an item could have a different tax rate.
    		float qst = totalPrice * (dish.getQst() * qstRate / 100f);
    		
//@NOTE: the price is already the final item totalprice (even the discount calculated), so no need to devide again.
//    		if(pS > 0) {
//    			price /= pS;
//    			gst /= pS;
//    			qst /= pS;
//    		}
//    		if(pK > 0) {
//    			price /= pK;
//    			gst /= pK;
//    			qst /= pK;
//    		}
    		
    		subTotal += totalPrice;
    		totalGst += gst;
    		totalQst += qst;
    	}
    	
    	subTotal -= discount;
    	subTotal += serviceFee;
		
    	if(BarOption.isDiscountAffectTax()) {
    		totalGst -= Math.round(discount * gstRate / 100f);
    		totalQst -= Math.round(discount * qstRate / 100f);
    	}
    	
    	if(BarOption.isServiceFeeAffectTax()) {
    		totalGst += Math.round(serviceFee * gstRate / 100f);
    		totalQst += Math.round(serviceFee * qstRate / 100f);
    	}
    	totalGst = Math.round(totalGst);
    	totalQst = Math.round(totalQst);
    	lblDiscount.setText(discount > 0 ? BarFrame.consts.Discount() + " : -" + BarOption.getMoneySign() + BarUtil.formatMoney((discount)/100f) : "");
    	lblServiceFee.setText(serviceFee > 0 ? BarFrame.consts.ServiceFee() + " : " + BarOption.getMoneySign() + BarUtil.formatMoney((serviceFee)/100f) : "");
    	lblSubTotle.setText(BarFrame.consts.Subtotal() + " : " + BarOption.getMoneySign() + BarUtil.formatMoney(subTotal/100f));
    	lblTPS.setText(BarFrame.consts.GST() + " : " + BarOption.getMoneySign() + BarUtil.formatMoney(totalGst/100f));
    	lblTVQ.setText(BarFrame.consts.QST() + " : " + BarOption.getMoneySign() + BarUtil.formatMoney(totalQst/100f));
        int total = Math.round(subTotal + totalGst + totalQst);
        valTotlePrice.setText(BarUtil.formatMoney((total)/100f));
        
        BarFrame.setStatusMes(BarFrame.consts.getPennyRounded() + BarUtil.canadianPennyRound(valTotlePrice.getText()));

		if(BarFrame.secondScreen != null) {
			BillPanel billPanel2 = BarFrame.customerFrame.billPanel;
			billPanel2.lblDiscount.setText(lblDiscount.getText());
			billPanel2.lblServiceFee.setText(lblServiceFee.getText());
			billPanel2.lblSubTotle.setText(lblSubTotle.getText());
			billPanel2.lblTPS.setText(lblTPS.getText());
			billPanel2.lblTVQ.setText(lblTVQ.getText());
			billPanel2.valTotlePrice.setText(valTotlePrice.getText());
			
			BarFrame.customerFrame.updateTotal(valTotlePrice.getText());
		}
    }
    
    public void initContent() {
    	String billIndex = billButton != null ? billButton.getText() : BarFrame.instance.getCurBillIndex();
		//used deleted <= 1, means both uncompleted and normally completed will be displayed, unnormally delted recored will be delted = 100
		String tableName = BarFrame.instance.cmbCurTable.getSelectedItem().toString();
		String openTime = BarFrame.instance.valStartTime.getText();
		String billId = BarFrame.instance.isShowingAnExpiredBill ? String.valueOf(BarFrame.instance.getCurBillID()) : "";
		
		initContent(billId, billIndex, tableName, openTime);
	}
    
    public void initContent(String billId, String billIndex, String tableName, String openTime) {
    	if((billId == null || "".equals(billId)) && (openTime == null || "".equals(openTime))) {//we can find bill by id, if no id, then openTime is a "must have" to locate a sigle bill.
    		int tColCount = table.getColumnCount();
    		Object[][] tValues = new Object[0][tColCount];
    		table.setDataVector(tValues, header);
    		updateTotalArea();
    		resetColWidth(scrContent.getWidth());
    		return;
    	}
    	
    	resetProperties();
    	
    	//get outputs of current table and bill id.
    	StringBuilder sql = null;
		try {
			orderedDishAry = initTableWithOutput(sql, billId, billIndex, tableName, openTime);	//make the table display the dishes in this bill first. (output of dumped bill will not match billId.)

			if(orderedDishAry.size() > 0 && orderedDishAry.get(0).getBillID() > 0) {	//if has output, update the discount and service fee, and tip info, and (don't forget the billID).
																						//then get the billID from any output, 
				int billID = initBillStatus(sql, billIndex, tableName, openTime);
				setBillID(billID);	//@NOTE: do not use orderedDishAry.get(0).getBillID() to get billID, because when combine all we don't modify bill id in output and dish (for undo use)
			
			}else if(tableName.length() > 0 && openTime.length() > 0) {		//if has no output(---could be an non-first but empty bill), then if has tablename and opentime
																			//together with the BillIdx in Barframe, we can still locate a bill.
				int billID = getBillIdByIdxOrCreateNew(sql, Integer.valueOf(billIndex), tableName, openTime);			//what kind of case will reach here: no bill and create an new empty bill?
				setBillID(billID);
			}
			
			// do not set the default selected value, if it's used in billListDlg.
			if (salesPanel != null && orderedDishAry.size() > 0) {
				table.setSelectedRow(orderedDishAry.size() - 1);
			}
		} catch (Exception e) {
			L.e("BillPanel", " exception when initContent()" + sql, e);
		}

		updateTotalArea();
	    setBackground(status >= DBConsts.completed || status < DBConsts.original ? Color.gray : null);
		//reset the flag whichi is only used for showing expired bills.
		BarFrame.instance.isShowingAnExpiredBill = false;
	}

    private ArrayList<Dish> initTableWithOutput(StringBuilder sql, String billId, String billIndex, String tableName, Object openTime) throws SQLException {
    	ArrayList<Dish> dishAry = new ArrayList<Dish>();
    	sql = new StringBuilder("select * from OUTPUT, PRODUCT where OUTPUT.SUBJECT = '")
			.append(tableName)
			.append("' and CONTACTID = ").append(billIndex)
			.append(" and (deleted is null or deleted < ").append(BarFrame.instance.isShowingAnExpiredBill ? DBConsts.deleted : DBConsts.expired)	//dumpted also should show.
			.append(") AND OUTPUT.PRODUCTID = PRODUCT.ID and output.time = '")
			.append(openTime).append("'").append(billId != null && billId.length() > 0 ? " and output.category = " + billId : "");	//new added outputs after dumped should not display.
		
		ResultSet rs = PIMDBModel.getReadOnlyStatement().executeQuery(sql.toString());
		rs.afterLast();
		rs.relative(-1);
		int tmpPos = rs.getRow();

		int tColCount = table.getColumnCount();
		Object[][] tValues = new Object[tmpPos][tColCount];
		rs.beforeFirst();
		tmpPos = 0;
		while (rs.next()) {
			Dish dish = new Dish();
			dish.setCATEGORY(rs.getString("PRODUCT.CATEGORY"));
			dish.setDiscount(rs.getInt("OUTPUT.discount"));//
			dish.setDspIndex(rs.getInt("PRODUCT.INDEX"));
			dish.setGst(rs.getInt("PRODUCT.FOLDERID"));
			dish.setId(rs.getInt("PRODUCT.ID"));
			dish.setLanguage(0, rs.getString("PRODUCT.CODE"));
			dish.setLanguage(1, rs.getString("PRODUCT.MNEMONIC"));
			dish.setLanguage(2, rs.getString("PRODUCT.SUBJECT"));
			dish.setModification(rs.getString("OUTPUT.CONTENT"));//
			dish.setNum(rs.getInt("OUTPUT.AMOUNT"));//
			dish.setOutputID(rs.getInt("OUTPUT.ID"));//
			dish.setPrice(rs.getInt("PRODUCT.PRICE"));
			dish.setPrinter(rs.getString("PRODUCT.BRAND"));
			dish.setPrompMenu(rs.getString("PRODUCT.UNIT"));
			dish.setPrompMofify(rs.getString("PRODUCT.PRODUCAREA"));
			dish.setPrompPrice(rs.getString("PRODUCT.CONTENT"));
			dish.setQst(rs.getInt("PRODUCT.STORE"));
			dish.setSize(rs.getInt("PRODUCT.COST"));
			dish.setBillIndex(billIndex);
			dish.setOpenTime(rs.getString("OUTPUT.TIME"));	//output time is table's open time. no need to remember output created time.
			dish.setBillID(rs.getInt("OUTPUT.Category"));
			dish.setTotalPrice(rs.getInt("OUTPUT.TOLTALPRICE"));
			dishAry.add(dish);
			
			tValues[tmpPos][0] = dish.getDisplayableNum(dish.getNum());
			
			tValues[tmpPos][1] = dish.getLanguage(LoginDlg.USERLANG);

			String[] marks = dish.getModification().split(BarDlgConst.delimiter);
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < marks.length; i++) {
				String[] langs = marks[i].split(BarDlgConst.semicolon);
				String lang = langs.length > LoginDlg.USERLANG ? langs[LoginDlg.USERLANG] : langs[0];
				if(lang.length() == 0 || "null".equalsIgnoreCase(lang)) {
					lang = langs[0].length() == 0 || "null".equalsIgnoreCase(lang) ? "" : langs[0];
				}
				sb.append(lang).append(" ");
			}
			if(dish.getDiscount() == 0) {
				tValues[tmpPos][2] = sb.toString();
			}else{
				tValues[tmpPos][2] = sb.append("  -").append(BarOption.getMoneySign()).append(BarUtil.formatMoney(dish.getDiscount() / 100.0)).toString();
			}
			
			tValues[tmpPos][3] =  BarOption.getMoneySign() + dish.getTotalPrice() / 100f;
			tmpPos++;
		}

		table.setDataVector(tValues, header);
		
		resetColWidth(scrContent.getWidth());

        if(BarFrame.secondScreen != null) {
        	BarFrame.customerFrame.billPanel.resetColWidth(scrContent.getWidth());
        }
		return dishAry;
	}

	private int initBillStatus(StringBuilder sql, String billIndex, String tableName, String openTime) throws SQLException {
		sql = new StringBuilder("select * from Bill where opentime = '").append(openTime)
				.append("' and billIndex = '").append(billIndex).append("'")
				.append(" and tableID = '").append(tableName).append("'")
				.append(" and (status is null or status ").append(BarFrame.instance.isShowingAnExpiredBill ? "<=" : "<").append(DBConsts.expired).append(")");
		  
		ResultSet rs = PIMDBModel.getReadOnlyStatement().executeQuery(sql.toString());
		rs.beforeFirst();
		if(rs.next()) {
		    discount = rs.getInt("discount");
		    serviceFee = rs.getInt("serviceFee");
		    tip = rs.getInt("tip");
		    cashback = rs.getInt("cashback");
		    status = rs.getInt("status");
		    comment = rs.getString("comment");
		    return rs.getInt("id");
		}
		return -1;
	}

	private int getBillIdByIdxOrCreateNew(StringBuilder sql, int billIdx, String tableName, String openTime) throws SQLException {
		sql = new StringBuilder("Select id from bill where tableID = '").append(tableName)
				.append("' and opentime = '").append(openTime)
				.append("' and billIndex = '").append(billIdx).append("'");
		
		ResultSet resultSet = PIMDBModel.getReadOnlyStatement().executeQuery(sql.toString());
		
		resultSet.beforeFirst();
		if(resultSet.next()) {
			return resultSet.getInt("id");
		}else {
			// it's not error, can be openning a new bill by clicking an new table. what we should do is add a new bill.
			//L.e("initing BillPanel", "there's no bill in an openned table.", null);
			return BarFrame.instance.createAnEmptyBill("", openTime, billIdx);
		}
	}
	
	private void resetProperties(){
        orderedDishAry.clear();
        discount = 0;
        tip = 0;
        serviceFee = 0;
        received = 0;
        cashback = 0;
        //set bill ID to 0 is not helpful, it should be updated in each call. setBillID(0);
        comment = "";
        status = DBConsts.original;
        setBackground(null);
        
        totalGst = 0;
    	totalQst = 0;
    	subTotal = 0;
        
    }
    
    public void resetColWidth(int tableWidth) {
    	
        PIMTableColumn tmpCol1 = table.getColumnModel().getColumn(0);
        tmpCol1.setWidth(60);
        tmpCol1.setPreferredWidth(60);
        
        PIMTableColumn tmpCol4 = table.getColumnModel().getColumn(3);
        tmpCol4.setWidth(120);
        tmpCol4.setPreferredWidth(120);
        
        //at first, teh tableWidth is 0, then after, the tableWidth will be 260. 
        PIMTableColumn tmpCol2 = table.getColumnModel().getColumn(1);
        int width = (tableWidth - tmpCol1.getWidth() - tmpCol4.getWidth()) / 2 + 3;
        tmpCol2.setWidth(BarOption.getNameColWidth(width));
        tmpCol2.setPreferredWidth(BarOption.getNameColWidth(width));
        
        PIMTableColumn tmpCol3 = table.getColumnModel().getColumn(2);
        width = (tableWidth - tmpCol1.getWidth() - tmpCol2.getWidth() - tmpCol4.getWidth()) - 3;
        tmpCol3.setWidth(width - (scrContent.getVerticalScrollBar().isVisible() ? scrContent.getVerticalScrollBar().getWidth() : 0));
        tmpCol3.setPreferredWidth(width - (scrContent.getVerticalScrollBar().isVisible() ? scrContent.getVerticalScrollBar().getWidth() : 0));
        
        table.setRowHeight(BarOption.getTableRowHeight());
        
        table.validate();
        table.revalidate();
        table.invalidate();
    }

    void removeFromSelection(int selectedRow) {
    	
		int tValidRowCount = getUsedRowCount(); // get the used RowCount
    	if(selectedRow < 0 || selectedRow > tValidRowCount - 1) {
    		JOptionPane.showMessageDialog(this, BarFrame.consts.OnlyOneShouldBeSelected());
    		ErrorUtil.write("Unexpected row number when calling removeAtSelection : " + selectedRow);
    		return;
    	}
    	
    	//update db first
    	Dish dish = orderedDishAry.get(selectedRow);
    	if(dish.getOutputID() > -1) {
    		dish.changeOutputStatus(comment.contains(PrintService.OLD_SUBTOTAL) ? DBConsts.expired : DBConsts.deleted);
    	}
    	//update array second.
		orderedDishAry.remove(selectedRow);

    	if(dish.getRuleMark() != null) {
    		checkWithRules();
    	}
    	
		//update the table view
		int tColCount = table.getColumnCount();
		Object[][] tValues = new Object[tValidRowCount - 1][tColCount];
		for (int r = 0; r < tValidRowCount; r++) {
			if(r == selectedRow) {
				continue;
			}else {
				int rowNum = r > selectedRow ? r : r + 1;
			    for (int c = 0; c < tColCount; c++)
			        tValues[rowNum-1][c] = table.getValueAt(r, c);
			}
		}
		table.setDataVector(tValues, header);
		resetColWidth(scrContent.getWidth());
        if(BarFrame.secondScreen != null) {
        	BarFrame.customerFrame.billPanel.resetColWidth(scrContent.getWidth());
        }
		table.setSelectedRow(tValues.length - 1); //@Note this will trigger a value change event, to set the curDish.
		updateTotalArea();
	}
    
    //return true means can move on, return false means user don't want to move on.
	public boolean checkStatus() {
		if(status >= DBConsts.billPrinted || status < DBConsts.original) {//check if the bill is more than billPrinted.
			if (JOptionPane.showConfirmDialog(this, BarFrame.consts.ConvertClosedBillBack(), BarFrame.consts.Operator(),
		            JOptionPane.YES_NO_OPTION) != 0) {// are you sure to convert the voided bill back？
		        return false;
			}else {
				reGenerate(null);
			}
		}
		return true;
	}
	
	//caller can specify the billIdx, if the billIdx is not specified, then use the billIdx on Frame.
	public void reGenerate(String billIdx) {
		if(billIdx == null || billIdx.length() == 0) {
			billIdx = BarFrame.instance.getCurBillIndex();
		}
		
		try {
			//dump the old bill and create a new bill
			StringBuilder sql = new StringBuilder("update bill set status = ").append(DBConsts.expired)
	 				.append(" where id = ").append(getBillID());
	 		PIMDBModel.getStatement().executeUpdate(sql.toString());
	 		
	 		//Generate new bill with ref to dumped bill everything else use the data on current billPane
	 		//@NOTE:no need to generate new output. because the output will be choose by table and billIdx, so old output will go to new bill automatically.
	 		//while, if user open the dumped old bill, then the removed item will be disappears and new added item will appear on old bill also.
	 		//this will be a known bug. TDOO:we can make it better by searching output by billID when it's a dumped bill. hope no one will need to check the dumped bills.
	 		//??what do we do when removing an saved item from billPanel?
	 		if(status >= DBConsts.billPrinted || status < DBConsts.original) {
		 		StringBuilder newComment = new StringBuilder(PrintService.REF_TO).append(getBillID());
				if(status >= DBConsts.completed || status < DBConsts.original) {	//if already paid, then need to know old moneys, so in mev can report how much added or returned.
					newComment.append("F");	
				}
				newComment.append("\n").append(PrintService.OLD_SUBTOTAL).append(BarUtil.formatMoney(subTotal / 100.0));	//this value will be needed anyway.
				if(status >= DBConsts.completed || status < DBConsts.original) {	//@Note mess the order of each money.
					newComment.append("\n").append(PrintService.OLD_GST).append(BarUtil.formatMoney(totalGst / 100.0))
						.append("\n").append(PrintService.OLD_QST).append(BarUtil.formatMoney(totalQst / 100.0))
						.append("\n").append(PrintService.OLD_TOTAL).append(valTotlePrice.getText());
				}
				
				//as long as a bill is regenerated, the status of the bill must be at least billprinted.
				//now the problem is shall we remember the old price? if we append the old moneys, then when we calculate the mtTransAvTaxes, sub total will minus 
				//the oldSubtotal, which means the mtTransAVTaxes will eventurally be the differents----this is good for modified invoice, not for modified check.
				//so we currently decide only when it's completed, we append the old moneys. that mean, when we are calculating a mtTransAvTaxes for checks, we
				//should remenber that there's only one old money(subtotal) in end message if it's not completed, but bill printed....we have to keep this one because 
				//when the check modified, we need to know the old subtotal in ref part.
				this.comment = newComment.toString();	//set the comment property, so when creating a new bill base on current one, will copy the comment into the new bill.
			}else {	//according to revenue test case, if it's refunded bill, when regenerate a bill base on it, should have not ref part, (personally don't understand it yet).
				this.comment = "";
			}
	 		
	 		int newBillID = cloneCurrentBillRecord(BarFrame.instance.cmbCurTable.getSelectedItem().toString(),
					billIdx,
					BarFrame.instance.valStartTime.getText(),
					Math.round(Float.valueOf(valTotlePrice.getText()) * 100));
	 		
	        //when we reopen a refunded bill, we create a new bill which is a original bill, so we must clean the received money with the refund count.

	 		if(status < DBConsts.original){	//save the old money numbers, in case the old status is negative(means have returned some money.
	 			StringBuilder sb = new StringBuilder("select * from bill where id = " + newBillID);

 	    		ResultSet rs = PIMDBModel.getReadOnlyStatement().executeQuery(sb.toString());
 	            rs.next();
 	        	
 	            int oldTotal = rs.getInt("total");
 	            int oldCashReceived =rs.getInt("cashReceived");
 	            int oldDebitReceived = rs.getInt("debitReceived");
 	            int oldVisaReceived = rs.getInt("visaReceived");
 	            int oldMasterReceived = rs.getInt("masterReceived");
 	        	
 	            //clean cashback and tip first, and clean status at the same time.
 	        	//set status = 0 immediatly when refund is covered by received money, in case another receive can also cover the refund, will be minus twice.
 	            int newCashReceived = oldCashReceived + cashback;	//cashReceived is always related with cashback.
	        	if(newCashReceived + status > 0) {
	        		newCashReceived += status;
	        		status = 0;
	        	}else {
	        		status += newCashReceived;
	        		newCashReceived = 0;
	        	}
 	            
 	        	//debitReceived
 	        	int newDebitReceived = oldDebitReceived;
 	        	if(oldDebitReceived > tip) {
 	        		newDebitReceived -= tip;
 	        	}
 	        	if(newDebitReceived + status > 0) {
    				newDebitReceived += status;
    				status = 0;
    			} else {
    				status += newDebitReceived;
    				newDebitReceived = 0;
    			}
 	        	
 	        	//visaReceived
 	            int newVisaReceived = oldVisaReceived;
 	            if(newVisaReceived > tip) {
 	            	newVisaReceived -= tip;
 	        	}
    			if(newVisaReceived + status > 0) {
    				newVisaReceived += status;
    				status = 0;
    			} else {
    				status += newVisaReceived;
    				newVisaReceived = 0;
    			}
 	            
 	            //masterReceived
 	            int newMasterReceived = oldMasterReceived;
 	            if(newMasterReceived > tip) {
 	            	newMasterReceived -= tip;
 	        	}
    			if(newMasterReceived + status > 0) {
    				newMasterReceived += status;
    				status = 0;
    			} else {
    				status += newMasterReceived;
    				newMasterReceived = 0;
    			}
 	            
 	            sql = new StringBuilder("update bill set tip = 0, cashback = 0, status = 0, cashReceived = ").append(newCashReceived)
 	            		.append(", debitReceived = ").append(newDebitReceived)
 	            		.append(", visaReceived = ").append(newVisaReceived)
 	            		.append(", masterReceived = ").append(newMasterReceived)
 	            		.append(" where id = ").append(newBillID);
 	           PIMDBModel.getStatement().executeUpdate(sql.toString());
	 		}
	 		
   			//tip must be able to set to 0.
	        tip = 0;
	        cashback = 0;
	 		status = DBConsts.original;	//will be used when clicking buttons, set to original, so will not trigger warning dialogs.
	 		setBillID(newBillID);			//will be used when adding new item into the bill
		}catch(Exception exp) {
			L.e("SalesPane", "Exception happenned when converting bill's status to 0", exp);
		}
	}
	
    private int getUsedRowCount() {
        for (int i = 0, len = table.getRowCount(); i < len; i++)
            if (table.getValueAt(i, 0) == null)
                return i; // 至此得到 the used RowCount。
        return table.getRowCount();
    }

    void reLayout() {
        int panelHeight = getHeight();

        int tBtnWidht = (getWidth() - CustOpts.HOR_GAP * 10) / 9;
        int tBtnHeight = panelHeight / 10;
        
     // table area-------------
        int poxX = 0;
        int posY = 0;
        int scrContentHeight = getHeight() - BarFrame.consts.TotalAreaHeight;
        if(billButton != null) {
        	billButton.setBounds(poxX, posY, getWidth(), CustOpts.BTN_HEIGHT + 16);
        	posY += billButton.getHeight();
        	scrContentHeight -= billButton.getHeight() - lblSubTotle.getPreferredSize().height;
        }
        scrContent.setBounds(poxX, posY, getWidth(), scrContentHeight);
        
		// sub total-------
		if(billButton == null){
        	btnMore.setBounds(scrContent.getX() + scrContent.getWidth() - BarFrame.consts.SCROLLBAR_WIDTH,
        			scrContent.getY() + scrContent.getHeight() + CustOpts.VER_GAP, 
            		BarFrame.consts.SCROLLBAR_WIDTH, BarFrame.consts.SCROLLBAR_WIDTH);
            btnLess.setBounds(btnMore.getX() - CustOpts.HOR_GAP - BarFrame.consts.SCROLLBAR_WIDTH, btnMore.getY(), 
            		BarFrame.consts.SCROLLBAR_WIDTH, BarFrame.consts.SCROLLBAR_WIDTH);
            
    		valTotlePrice.setBounds(scrContent.getX(), scrContent.getY() + scrContent.getHeight() + CustOpts.VER_GAP,
    				btnLess.getX() - scrContent.getX() - 5, BarFrame.consts.TotalAreaHeight - CustOpts.VER_GAP - 3);
    		
        }else {
    		valTotlePrice.setBounds(scrContent.getX(), scrContent.getY() + scrContent.getHeight() + CustOpts.VER_GAP,
    				scrContent.getWidth() - 5, BarFrame.consts.TotalAreaHeight - CustOpts.VER_GAP - 3);
        }
        lblSubTotle.setBounds(scrContent.getX(), scrContent.getY() + scrContent.getHeight() + 40 + CustOpts.VER_GAP * 2,
        		scrContent.getWidth() / 3, lblSubTotle.getPreferredSize().height);
        lblDiscount.setBounds(scrContent.getX(), scrContent.getY() + scrContent.getHeight() + CustOpts.VER_GAP, 
        		scrContent.getWidth() / 5, lblSubTotle.getHeight());
        lblServiceFee.setBounds(lblDiscount.getX(), lblDiscount.getY() + lblDiscount.getHeight() + CustOpts.VER_GAP, 
        		scrContent.getWidth() / 5, lblDiscount.getHeight());
        
		lblTPS.setBounds(scrContent.getX() + scrContent.getWidth() / 5, lblDiscount.getY(),
				scrContent.getWidth() / 5, lblTPS.getPreferredSize().height);
		lblTVQ.setBounds(lblTPS.getX(), lblTPS.getY() + lblTPS.getHeight() + CustOpts.VER_GAP,
				scrContent.getWidth() / 5, lblTVQ.getPreferredSize().height);
		
    }
    
    void initComponent() {
    	removeAll();
        table = new PIMTable();// 显示字段的表格,设置模型
        scrContent = new PIMScrollPane(table);
        lblDiscount = new JLabel();
        lblServiceFee = new JLabel();
        lblSubTotle = new JLabel(BarFrame.consts.Subtotal());
        lblTPS = new JLabel(BarFrame.consts.GST());
        lblTVQ = new JLabel(BarFrame.consts.QST());
        valTotlePrice = new JLabel();
        btnMore = new ArrowButton("<html><h1 style='text-align: center; padding-bottom: 5px; color:#18F507;'>+</h1></html>");
        btnLess = new ArrowButton("<html><h1 style='text-align: center; padding-bottom: 5px; color:#FB112C;'>-</h1></html>");
        
        Color bg = BarOption.getBK("Bill");
    	if(bg == null) {
    		bg = new Color(216,216,216);
    	}
        setBackground(bg);
        setLayout(null);
													//in that method will check if the mirrorTable exist.
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setAutoscrolls(true);
        table.setCellEditable(false);
        table.setRenderAgent(this);
        table.setHasSorter(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        table.setFont(BarOption.getSelectionFont());
        
        table.setDataVector(new Object[0][header.length], header);
        DefaultPIMTableCellRenderer tCellRender = new DefaultPIMTableCellRenderer();
        tCellRender.setOpaque(true);
        tCellRender.setBackground(Color.LIGHT_GRAY);
        table.getColumnModel().getColumn(1).setCellRenderer(tCellRender);
        valTotlePrice.setFont(BarOption.bigFont);
        valTotlePrice.setHorizontalAlignment(SwingConstants.RIGHT);
        //@do_not_work! valTotlePrice.setHorizontalAlignment(SwingConstants.RIGHT);
        //@work! valTotlePrice.setAlignmentX(Component.RIGHT_ALIGNMENT);
      //@do_not_work!valTotlePrice.setBackground(Color.RED);
      //@do_not_work!valTotlePrice.setOpaque(false);
        
        JLabel tLbl = new JLabel();
        tLbl.setOpaque(true);
        tLbl.setBackground(Color.GRAY);
        scrContent.setCorner(JScrollPane.LOWER_RIGHT_CORNER, tLbl);
        Font tFont = PIMPool.pool.getFont((String) CustOpts.custOps.hash2.get(PaneConsts.DFT_FONT), Font.PLAIN, 40);

        // Margin-----------------
        btnMore.setMargin(new Insets(0,0,0,0));
        btnLess.setMargin(btnMore.getInsets());
        
        // border----------
        table.setBorder(null);
        table.setShowGrid(false);
        // forcus-------------
        table.setFocusable(false);
        btnMore.setFocusable(false);
        btnLess.setFocusable(false);
        
        // disables
        btnMore.setEnabled(false);
        btnLess.setEnabled(false);
        
        // built
        if(billButton != null) {
        	add(billButton);
        	billButton.addActionListener(this);
        }else {
            add(btnMore);
            add(btnLess);
            add(lblDiscount);
            add(lblServiceFee);
            add(lblSubTotle);
            add(lblTPS);
            add(lblTVQ);
        }
        add(valTotlePrice);
        add(scrContent);

        addComponentListener(this);
        btnMore.addActionListener(this);
        btnLess.addActionListener(this);
        table.addMouseMotionListener(this);
        table.addMouseListener(this);
        table.getSelectionModel().addListSelectionListener(this);
        scrContent.getViewport().addMouseListener(this);
		reLayout();
    }
    
    public int getBillID() {
		return billID;
	}

	public void setBillID(int billID) {
		this.billID = billID;
	}
	
	public static void updateBill(int billId, String fieldName, int value) {
		StringBuilder sb = new StringBuilder("update bill set ").append(fieldName).append(" = ").append(value).append(" where id = ").append(billId);
		
		try {
			PIMDBModel.getStatement().executeUpdate(sb.toString());
		}catch(Exception e) {
			ErrorUtil.write(e);
		}
	}

	//
	public static void updateBillRecordPrices(BillPanel billPanel) {
		updateBill(billPanel.getBillID(), "total", Math.round(Float.valueOf(billPanel.valTotlePrice.getText()) * 100));
		updateBill(billPanel.getBillID(), "discount", Math.round(billPanel.discount));
		updateBill(billPanel.getBillID(), "serviceFee", billPanel.serviceFee);
	}

	public PIMTable table;
	public PIMScrollPane scrContent;

    public JLabel lblSubTotle;
    public JLabel lblTPS;
    public JLabel lblTVQ;
    public JLabel lblDiscount;
    public JLabel lblServiceFee;
    public JLabel valTotlePrice;
    
    private ArrowButton btnMore;
    private ArrowButton btnLess;
    
    public String[] header = new String[] {BarFrame.consts.Count(), BarFrame.consts.ProdName(), "", BarFrame.consts.Price()};

}
