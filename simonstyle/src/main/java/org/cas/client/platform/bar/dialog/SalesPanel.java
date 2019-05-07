package org.cas.client.platform.bar.dialog;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.cas.client.platform.bar.BarUtil;
import org.cas.client.platform.bar.action.UpdateItemDiscountAction;
import org.cas.client.platform.bar.action.UpdateItemPriceAction;
import org.cas.client.platform.bar.dialog.modifyDish.AddModificationDialog;
import org.cas.client.platform.bar.dialog.statistics.CheckBillDlg;
import org.cas.client.platform.bar.dialog.statistics.ReportDlg;
import org.cas.client.platform.bar.model.DBConsts;
import org.cas.client.platform.bar.model.Dish;
import org.cas.client.platform.bar.print.PrintService;
import org.cas.client.platform.bar.uibeans.CategoryToggleButton;
import org.cas.client.platform.bar.uibeans.FunctionButton;
import org.cas.client.platform.bar.uibeans.MenuButton;
import org.cas.client.platform.cascustomize.CustOpts;
import org.cas.client.platform.casutil.ErrorUtil;
import org.cas.client.platform.casutil.L;
import org.cas.client.platform.pimmodel.PIMDBModel;
import org.cas.client.resource.international.DlgConst;

//Identity表应该和Employ表合并。
public class SalesPanel extends JPanel implements ComponentListener, ActionListener, FocusListener {

	String[][] categoryNameMetrix;
    ArrayList<ArrayList<CategoryToggleButton>> onSrcCategoryTgbMatrix = new ArrayList<ArrayList<CategoryToggleButton>>();
    CategoryToggleButton tgbActiveCategory;
    
    //Dish is more complecated than category, it's devided by category first, then divided by page.
    String[][] dishNameMetrix;// the struction must be [3][index]. it's more convenient than [index][3]
    String[][] onScrDishNameMetrix;// it's sub set of all menuNameMetrix
    private ArrayList<ArrayList<MenuButton>> onSrcMenuBtnMatrix = new ArrayList<ArrayList<MenuButton>>();

    //for print
    public static String SUCCESS = "0";
    public static String ERROR = "2";
    
    
    public SalesPanel() {
        initComponent();
    }

    // ComponentListener-----------------------------
    /** Invoked when the component's size changes. */
    @Override
    public void componentResized(
            ComponentEvent e) {
        reLayout();
    }

    /** Invoked when the component's position changes. */
    @Override
    public void componentMoved(ComponentEvent e) {}

    /** Invoked when the component has been made visible. */
    @Override
    public void componentShown(ComponentEvent e) {}

    /** Invoked when the component has been made invisible. */
    @Override
    public void componentHidden(ComponentEvent e) {}

    @Override
    public void focusGained(FocusEvent e) {
        Object o = e.getSource();
        if (o instanceof JTextField)
            ((JTextField) o).selectAll();
    }

    @Override
    public void focusLost(FocusEvent e) {}

    // ActionListner-------------------------------
    @Override
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();
        //FunctionButton------------------------------------------------------------------------------------------------
        if (o instanceof FunctionButton) {
        	if(o == btnCASH || o == btnDEBIT || o == btnVISA || o == btnMASTER || o == btnOTHER) { //pay

        		createAndPrintNewOutput();	//process the new added items (send to printer and db).
    			billPricesUpdateToDB();		//the total price could has changed, because user added new item.
        		
        		//if it's already paid, show comfirmDialog.
        		if(billPanel.status >= DBConsts.completed || billPanel.status < DBConsts.original) {
        			if(JOptionPane.showConfirmDialog(BarFrame.instance, BarFrame.consts.ConfirmPayAgain(), DlgConst.DlgTitle, JOptionPane.YES_NO_OPTION) != 0) {
            			return;
        			}else {
        				billPanel.reGenerate(null);
        			}
        		}
        		//check if the pay dialog is already visible, if yes, then update bill received values.
        		if(BarFrame.payDlg.isVisible()) {
        			BarFrame.payDlg.updateBill(billPanel.getBillID());
        		}
        		//show dialog-------------------------------------
         		BarFrame.payDlg.setFloatSupport(true);
         		if(o == btnCASH) {
         			BarFrame.payDlg.setTitle(BarFrame.consts.EnterCashPayment());
         		}else if(o == btnDEBIT) {
         			BarFrame.payDlg.setTitle(BarFrame.consts.EnterDebitPayment());
         		}else if(o == btnVISA) {
         			BarFrame.payDlg.setTitle(BarFrame.consts.EnterVisaPayment());
         		}else if(o == btnMASTER) {
         			BarFrame.payDlg.setTitle(BarFrame.consts.EnterMasterPayment());
         		}else if(o == btnOTHER) {
         			BarFrame.payDlg.setTitle(BarFrame.consts.EnterOtherPayment());
         		}
         		//init payDialog content base on bill.
         		BarFrame.payDlg.initContent(billPanel);
         		BarFrame.payDlg.setVisible(true);
        		
        	} else if (o == btnSplitBill) {		//split bill
        		//check if there unsaved dish, and give warning.
        		if(BillListPanel.curDish == null) {
            		if(JOptionPane.showConfirmDialog(BarFrame.instance, BarFrame.consts.UnSavedRecordFound(), DlgConst.DlgTitle, JOptionPane.YES_NO_OPTION) != 0)
            			return;
        		}
        		List<Dish> newDishes = billPanel.getNewDishes();
        		if(newDishes.size() > 0) {
        			for (Dish dish : newDishes) {
						if(dish.getPrinter() != null && dish.getPrinter().length() > 1) {
		        			JOptionPane.showMessageDialog(this, BarFrame.consts.UnSendRecordFound());
		        			return;
						}
					}
        		}
        		
        		createAndPrintNewOutput();
    			billPricesUpdateToDB();
        		BarFrame.instance.switchMode(1);
        		
        	} else if (o == btnRemoveItem) {	//remove item.
        		if(!billPanel.checkStatus()) {
            		return;
            	}
        		removeItem();
        		
        	} else if(o == btnModify) {	//Modify
        		//if there's a curDish?
        		if(BillListPanel.curDish == null) {//check if there's an item selected.
        			JOptionPane.showMessageDialog(this, BarFrame.consts.OnlyOneShouldBeSelected());
        			return;
        		}
        		new AddModificationDialog(BarFrame.instance, BillListPanel.curDish.getModification()).setVisible(true);
        		
        	}else if(o == btnServiceFee) {	//service fee
         		BarFrame.numberPanelDlg.setTitle(BarFrame.consts.ServiceFee());
         		BarFrame.numberPanelDlg.setNotice(BarFrame.consts.ServiceFeeNotice());
        		BarFrame.numberPanelDlg.setBtnSource(null);
         		BarFrame.numberPanelDlg.setFloatSupport(true);
         		BarFrame.numberPanelDlg.setPercentSupport(true);
         		
         		BarFrame.numberPanelDlg.setModal(true);
				BarFrame.numberPanelDlg.reLayout();
         		BarFrame.numberPanelDlg.setVisible(true);
         		
         		try {
     				String curContent = NumberPanelDlg.curContent;
     				if(curContent == null || curContent.length() == 0) {
     					return;
     				}
     				if(!billPanel.checkStatus()) {
     					return;
     				}
     				billPanel.serviceFee  = BarFrame.numberPanelDlg.isPercentage ? 
             				Math.round(Float.valueOf((billPanel.subTotal - billPanel.serviceFee + billPanel.discount) * Float.valueOf(curContent)))
             				: Math.round(Float.valueOf(curContent) * 100);
             				
             		billPanel.updateTotleArea();
             		
             		createAndPrintNewOutput();
             		billPricesUpdateToDB();
             		
             	}catch(Exception exp) {
                 	JOptionPane.showMessageDialog(BarFrame.numberPanelDlg, DlgConst.FORMATERROR);
             		return;
             	}
        		
        	}else if (o == btnPrintBill) { // print bill
        		createAndPrintNewOutput();		//will send new added(not printed yet) dishes to kitchen.
        		billPricesUpdateToDB();
        		billPanel.printBill(BarFrame.instance.cmbCurTable.getSelectedItem().toString(),
        				BarFrame.instance.getCurBillIndex(),
        				BarFrame.instance.valStartTime.getText(),
        				true);
        		billPanel.initContent();
        		
            } else if (o == btnReturn) { // return
            	if(billPanel.getNewDishes().size() > 0) {
            		if(JOptionPane.showConfirmDialog(BarFrame.instance, 
            				BarFrame.consts.COMFIRMLOSTACTION(), DlgConst.DlgTitle, JOptionPane.YES_NO_OPTION) != 0) {
    	                 return;	
    	            }
            	}
            	if(BarOption.isFastFoodMode()) {
            		BarFrame.instance.userCheckOut();
            	}else {
            		BarFrame.instance.switchMode(0);
            	}
            	
            } else if(o == btnAddBill) {		//Add bill
            	//save unsaved output
            	createAndPrintNewOutput();
            	BarUtil.updateBillRecordPrices(billPanel);
            	addNewBillInCurTable();
        	} else if (o == btnCancelAll) { // cancel all---- if bill is empty, then check if table is empty, if yes, close current table. yes or not, all back to table view.
            	if(billPanel.orderedDishAry.size() > 0) {	//if not empty, remove all new added items.
            		int newDishQT = billPanel.getNewDishes().size();
            		if(newDishQT == 0) {
            			if(JOptionPane.showConfirmDialog(this, BarFrame.consts.NoNewSelectionToCancel(), DlgConst.DlgTitle,
    		                    JOptionPane.YES_NO_OPTION) != 0) {
            				return;
            			}else {
            				voidCurrentOrder();
            			}
            		}
            		
            		int lastSavedRow = billPanel.orderedDishAry.size() - 1 - newDishQT;
            		
            		//update array first.
            		for(int i = billPanel.orderedDishAry.size() - 1; i > lastSavedRow; i--) {
            			billPanel.orderedDishAry.remove(i);
            		}
            		//update the table view
            		int tColCount = billPanel.table.getColumnCount();
            		int tValidRowCount = billPanel.orderedDishAry.size(); // get the used RowCount
            		Object[][] tValues = new Object[tValidRowCount][tColCount];
            		for (int r = 0; r < tValidRowCount; r++) {
            			for (int c = 0; c < tColCount; c++)
            				tValues[r][c] = billPanel.table.getValueAt(r, c);
            		}
            		billPanel.table.setDataVector(tValues, billPanel.header);
            		billPanel.resetColWidth(billPanel.getWidth());
            		billPanel.table.setSelectedRow(tValues.length - 1);
            		billPanel.updateTotleArea();
            	}else if(!BarOption.isFastFoodMode()){
            		//@NOTE: we don't close current bill, because maybe there's output still have billID of this bill, all the empty bill will be closed when table closed.
            		//update bill and dining_table in db.
            		if(BarFrame.instance.isTableEmpty(null, null)) {
            			BarFrame.instance.closeATable(null, null);
            		}
            		BarFrame.instance.switchMode(0);
            	}
            	
            } else if (o == btnVoidOrder) { // void order include saved ones
            	//if there's no dish on it at all (including deleted outputs), delete the bill directly
            	voidCurrentOrder();
            } else if (o == btnOpenDrawer) {		//open drawer
            	PrintService.openDrawer();
            	
            } else if (o == btnDiscBill) {//disc bill
            	
            	if(!BarOption.isWaiterAllowedToDiscount()) {
            		if (!BarFrame.instance.adminAuthentication()) 
        				return;
            	}
            	
         		BarFrame.discountDlg.setTitle(BarFrame.consts.DISCOUNT_BILL());
         		BarFrame.discountDlg.setNotice(BarFrame.consts.VolumnDiscountNotice());
         		BarFrame.discountDlg.setBtnSource(null);
         		BarFrame.discountDlg.setFloatSupport(true);
         		BarFrame.discountDlg.setPercentSupport(true);
         		BarFrame.discountDlg.setModal(true);
         		BarFrame.discountDlg.setVisible(true);
         		
         		try {
     				String curContent = BarFrame.discountDlg.curContent;
     				if(curContent == null || curContent.length() == 0)
     					return;
             		float discount = BarFrame.discountDlg.isPercentage ? 
             				(billPanel.subTotal + billPanel.discount) * (Float.valueOf(curContent)/100f)
             				: Float.valueOf(curContent);
             		discountBill(Math.round(discount * 100));
             		
             	}catch(Exception exp) {
                 	JOptionPane.showMessageDialog(BarFrame.discountDlg, DlgConst.FORMATERROR);
             		return;
             	}
            }else if(o == btnRefund) {	//refund
            	//check if it's already paid.
            	if(billPanel.status != DBConsts.completed) {
            		JOptionPane.showMessageDialog(this, BarFrame.consts.NotPayYet());
            		return;
            	}
            		
         		BarFrame.numberPanelDlg.setTitle(BarFrame.consts.Refund());
         		BarFrame.numberPanelDlg.setNotice(BarFrame.consts.RefundNotice());
            	BarFrame.numberPanelDlg.setBtnSource(null);
         		BarFrame.numberPanelDlg.setFloatSupport(true);
         		BarFrame.numberPanelDlg.setPercentSupport(false);
         		BarFrame.numberPanelDlg.reLayout();
         		BarFrame.numberPanelDlg.setModal(true);
         		BarFrame.numberPanelDlg.setContents(billPanel.valTotlePrice.getText());
         		BarFrame.numberPanelDlg.setVisible(true);
         		
         		try {
     				String curContent = BarFrame.numberPanelDlg.curContent;
     				if(curContent == null || curContent.length() == 0)
     					return;
     				
             		float refund = BarFrame.numberPanelDlg.isPercentage ? 
             				Float.valueOf(billPanel.valTotlePrice.getText()) * Float.valueOf(curContent)
             				: Float.valueOf(curContent);
             		
             		if(refund > Float.valueOf(billPanel.valTotlePrice.getText())) {
             			JOptionPane.showMessageDialog(this, BarFrame.consts.InvalidInput());
             			return;
             		}
             		
             		// get out existing status.
             		int refundAmount = billPanel.status;
                    if(refundAmount < -1) {	//if already refund, then add into existing amount.
                    	if (JOptionPane.showConfirmDialog(BarFrame.instance, BarFrame.consts.AllreadyRefund() + BarOption.getMoneySign() + (0-refundAmount) / 100.0, DlgConst.DlgTitle,
    		                    JOptionPane.YES_NO_OPTION) != 0) {// allready refunded, sure to refund again?
    						return;
    					}else {
    						refundAmount -= (int)(refund * 100);
    					}
                    }else {		//first time refund, then set the refund.
                    	refundAmount = 0 - (int)(refund * 100);
                    }
                    
            		new ChangeDlg(BarFrame.instance, 
            				BarOption.getMoneySign() + BarUtil.formatMoney(refund)).setVisible(true); //it's a non-modal dialog.
            		
            		//dump the old bill and create a new bill
            		StringBuilder sql = new StringBuilder("update bill set status = ").append(DBConsts.expired)
             				.append(" where id = ").append(billPanel.getBillID());
             		PIMDBModel.getStatement().executeUpdate(sql.toString());
             		
             		//generat new bill with ref to dumpted bill everything else use the data on current billPane
             		//@NOTE:no need to generata new output. the output will be choosed by table and billIdx.
            		billPanel.comment = PrintService.REF_TO + billPanel.getBillID() + "F";
             		int newBillID = billPanel.cloneCurrentBillRecord(BarFrame.instance.cmbCurTable.getSelectedItem().toString(),
            				String.valueOf(BarFrame.instance.getCurBillIndex()),
            				BarFrame.instance.valStartTime.getText(),
            				Math.round(Float.valueOf(billPanel.valTotlePrice.getText()) * 100));
             		
             		//change something on cur billPane, then use it to print the refund bill, to let revenue know the store refund some money.
             		billPanel.setBillID(newBillID);
            		PrintService.exePrintRefund(billPanel, - (int)(refund * 100));
            		
            		//update the status with new refund amount for the new bill, so next time refund will base on new number.
             		sql = new StringBuilder("update bill set status = ").append(refundAmount)
             				.append(" where id = ").append(newBillID);
             		PIMDBModel.getStatement().executeUpdate(sql.toString());
            		
            		BarFrame.instance.switchMode(0);
             		PrintService.openDrawer();
             	}catch(Exception exp) {
                 	JOptionPane.showMessageDialog(BarFrame.numberPanelDlg, DlgConst.FORMATERROR);
             		return;
             	}
            	
            } else if (o == btnMore) {//more
            	new MoreButtonsDlg(this).show((FunctionButton)o);
            	
            } else if (o == btnSend) {//send
        		createAndPrintNewOutput();
        		billPricesUpdateToDB();
            	if(BarOption.isFastFoodMode()) {
        	    	BarFrame.instance.valStartTime.setText(BarOption.df.format(new Date()));
        	    	addNewBillInCurTable();
    		    }else {
    		    	BarFrame.instance.switchMode(0);
    		    }
            } else if(o == btnOTHER) {
            	String giftCardNumber  = JOptionPane.showInputDialog(null, BarFrame.consts.Account());
        		if(giftCardNumber == null || giftCardNumber.length() == 0)
        			return;
        		
        		StringBuilder sql = new StringBuilder("SELECT * from hardware where category = 2 and name = '").append(giftCardNumber)
        				.append("' and (status is null or status = 0)");
        		try {
        			ResultSet rs = PIMDBModel.getReadOnlyStatement().executeQuery(sql.toString());
        			rs.afterLast();
                    rs.relative(-1);
                    int tmpPos = rs.getRow();
                    if(tmpPos == 0) {	//if there's no this coupon number in database, then warning and return.
                    	JOptionPane.showMessageDialog(this, BarFrame.consts.InvalidCoupon());
                    	return;
                    }else {			//if the number is OK.
                    	//get out every field of first matching record.
                    	rs.beforeFirst();
                        tmpPos = 0;
                        rs.next();
                        int id = rs.getInt("id");
                        int category = rs.getInt("style");
                        String productCode = rs.getString("IP");
                        int value = rs.getInt("langType");
                        
                        //show up the payDialog, waiting for user to input money, after confirm, the money should be deduct from the account of this card
                        SalesPanel salesPanel = (SalesPanel)BarFrame.instance.panels[2];
                        BarFrame.payDlg.maxInput = (float)(value / 100.0);
                        salesPanel.actionPerformed(new ActionEvent(salesPanel.btnOTHER, 0, ""));
                        //how to know the number user inputed, and how to verify if it's bigger than the money left in card?
                        if (BarFrame.payDlg.inputedContent != null && BarFrame.payDlg.inputedContent.length() > 0) {
    	                    float usedMoneyQT = Math.round(Float.valueOf(BarFrame.payDlg.inputedContent) * 100);
    	                    sql = new StringBuilder("update hardware set langType = langType - ").append(usedMoneyQT)
    	                    		.append(" where id = ").append(id);
    	                    PIMDBModel.getStatement().executeUpdate(sql.toString());
                        }
                    }
        		}catch(Exception exp) {
        			L.e("Redeem Coupon", "exception happend when redeem coupon: " + sql, exp);
        		}
            }else if (o == btnDiscountCoupon) {
        		String couponCode  = JOptionPane.showInputDialog(null, BarFrame.consts.couponCode());
        		if(couponCode == null || couponCode.length() == 0)
        			return;
        		
        		StringBuilder sql = new StringBuilder("SELECT * from hardware where category = 1 and name = '").append(couponCode)
        				.append("' and (status is null or status = 0)");
        		try {
        			ResultSet rs = PIMDBModel.getReadOnlyStatement().executeQuery(sql.toString());
        			rs.afterLast();
                    rs.relative(-1);
                    int tmpPos = rs.getRow();
                    if(tmpPos == 0) {	//if there's no this coupon number in database, then warning and return.
                    	JOptionPane.showMessageDialog(this, BarFrame.consts.InvalidCoupon());
                    	return;
                    }else {			//if the number is OK.
                    	//get out every field of first matching record.
                    	rs.beforeFirst();
                        tmpPos = 0;
                        rs.next();
                        int id = rs.getInt("id");
                        int category = rs.getInt("style");
                        String productCode = rs.getString("IP");
                        int value = rs.getInt("langType");
                        
                        //check if the coupon can be applied on current bill
                        int langIdx = CustOpts.custOps.getUserLang();
                        ArrayList<String> nameAry = getAppliableDishNames(productCode, langIdx); //if nameAry is null, mean apply to whole bill.
                        ArrayList<Dish> dishesOnBill = billPanel.orderedDishAry;
                        ArrayList<Dish> matchedDishesOnBill = getMatchedItem(dishesOnBill, nameAry, langIdx);
                        //@NOTE: if nameAry and matchedDishesOnBill are null, mean apply to whole bill.
                        if(matchedDishesOnBill != null && matchedDishesOnBill.size() == 0) {
                        	JOptionPane.showMessageDialog(this, BarFrame.consts.couponNotApplyToBill());
                        	return;
                        }
                        
                        //if matchedDishesOnBill is null, then apply the coupon to the whole bill.
                        if(matchedDishesOnBill == null) {
    	                    if(category == 0) {//mean the price is absolute price, not percentage.
    	                    	int total = Math.round(Float.valueOf(billPanel.valTotlePrice.getText()) * 100);
    	                    	value = value > total ?  total : value;
    	                    	discountBill(value);
    	                    }else {
    	                    	value = Math.round((billPanel.subTotal + billPanel.discount) * (Float.valueOf(value/100f) / 100f));
    	                    	discountBill(value);
    	                    }
    	                    //recalculate the left
    	                    
    	                    //if the total is 0, then close cur bill.
    	                    if("0.00".equals(billPanel.valTotlePrice.getText())) {
    	                    	PrintService.exePrintInvoice(billPanel, false, true, true);
    	                    	BarFrame.instance.closeCurrentBill();
    		                	this.setVisible(false);
    		                	if(BarOption.isFastFoodMode()) {
    		            	    	BarFrame.instance.valStartTime.setText(BarOption.df.format(new Date()));
    		            	    	addNewBillInCurTable();
    		                	}else {
    			            		if(BarFrame.instance.isTableEmpty(null, null)) {
    			            			BarFrame.instance.closeATable(null, null);
    			            		}
    			            		BarFrame.instance.switchMode(0);
    		                	}
    	                    }
                        } else {//apply the coupon only to the dish item.
    	                	//find out the most expensive dish
                        	Dish mostExpensiveDish = null;
                        	for (Dish dish : matchedDishesOnBill) {
                        		mostExpensiveDish = mostExpensiveDish.getPrice() < dish.getPrice() ? dish : mostExpensiveDish;
    						}
                        	//calculate coupon value:
                        	if(category == 0) {//mean the price is absolute price, not persentage.
                        		value = value > mostExpensiveDish.getPrice() ? mostExpensiveDish.getPrice() : value;
    	                    }else {
    	                    	value = Math.round(mostExpensiveDish.getPrice() * (Float.valueOf(value) / 100f));
    	                    }
                        	
                        	discountADish(value, mostExpensiveDish);
                        }
                        
                    	//update the status of the coupon.
                    	sql = new StringBuilder("update hardware set status = 1 where id = ").append(id);
                    	PIMDBModel.getStatement().executeUpdate(sql.toString());
                    }
        		}catch(Exception exp) {
        			L.e("Redeem Coupon", "exception happend when redeem coupon: " + sql, exp);
        		}
            }else if (o == btnSetting) {
            	BarFrame.instance.switchMode(3);
            }else if (o == btnSuspend) {
            	createAndPrintNewOutput();
            	BarUtil.updateBillRecordPrices(billPanel);
            	
            	this.setVisible(false);
            	if(billPanel.status > DBConsts.suspended || billPanel.status < DBConsts.original) {
    				return;
    			}
    			
    	        try {
    	        	String tableID = BarFrame.instance.cmbCurTable.getSelectedItem().toString();
    	        	//update outputs
    				StringBuilder sql = new StringBuilder("update output set deleted = ").append(DBConsts.suspended)
    		                .append(" where SUBJECT = '").append(tableID)
    		                .append("' and time = '").append(BarFrame.instance.valStartTime.getText())
    		                .append("' and (deleted is null or deleted = ").append(DBConsts.original).append(")");
    				PIMDBModel.getStatement().executeUpdate(sql.toString());
    				
    				//update bills
    				sql = new StringBuilder("update bill set status = ").append(DBConsts.suspended)
    						.append(" where openTime = '").append(BarFrame.instance.valStartTime.getText())
    						.append("' and (status is null or status = ").append(DBConsts.original).append(")");
    				PIMDBModel.getStatement().executeUpdate(sql.toString());
    				
    	        }catch(Exception exp) {
    	        	ErrorUtil.write(exp);
    	        }
    	        
            	if(BarOption.isFastFoodMode()) {
        	    	((SalesPanel)BarFrame.instance.panels[2]).addNewBillInCurTable();
            	}else {
    				BarFrame.instance.setCurBillIdx("");
    				BarFrame.instance.switchMode(0);
            	}
            }else if (o == btnCheckOrder) {
            	String endNow = BarOption.df.format(new Date());
        		int p = endNow.indexOf(" ");
        		String startTime = endNow.substring(0, p + 1) + BarOption.getStartTime();
        		CheckBillDlg dlg = new CheckBillDlg(BarFrame.instance);
        		dlg.initContent(startTime, endNow);
        		dlg.setVisible(true);
            }else if(o == btnReport) {
            	ReportDlg dlg = new ReportDlg(BarFrame.instance);
        		dlg.setVisible(true);
            }
        }
        //JToggleButton-------------------------------------------------------------------------------------
        else if(o instanceof JToggleButton) {
        	 if (o == btnDiscItem) {	//disc item
         		if(BillListPanel.curDish == null) {//check if there's an item selected.
         			JOptionPane.showMessageDialog(this, BarFrame.consts.OnlyOneShouldBeSelected());
         			return;
         		}
         		
         		if(!BarOption.isWaiterAllowedToDiscount()) {
            		if (!BarFrame.instance.adminAuthentication()) 
        				return;
            	}
         		
         		BarFrame.discountDlg.setTitle(BarFrame.consts.DISCITEM());
         		BarFrame.discountDlg.setNotice(BarFrame.consts.DISC_ITEMNotice());
         		BarFrame.discountDlg.setBtnSource(btnDiscItem);//pomp up a discountDlg
         		BarFrame.discountDlg.setFloatSupport(true);
         		BarFrame.discountDlg.setPercentSupport(true);
         		BarFrame.discountDlg.setModal(false);
         		//should no record selected, select the last one.
         		BarFrame.discountDlg.setVisible(btnDiscItem.isSelected());	//@NOTE: it's not model mode.
         		BarFrame.discountDlg.setAction(new UpdateItemDiscountAction(btnDiscItem, billPanel));
         		
             }else if(o == btnChangePrice) {	//change price
            	if(BillListPanel.curDish == null) {//check if there's an item selected.
          			JOptionPane.showMessageDialog(this, BarFrame.consts.OnlyOneShouldBeSelected());
          			return;
          		}
            	if(!BarOption.isWaiterAllowedToChangePrice()) {
            		if (!BarFrame.instance.adminAuthentication()) 
        				return;
            	}
         		showPriceChangeDlg();
        	}
        }
    }

	private void voidCurrentOrder() {
		int dishLength = billPanel.orderedDishAry.size();
		int billID = billPanel.getBillID();
    	String curBill = BarFrame.instance.getCurBillIndex();
    	
		try {
			//check if it's a "mistake-opening-table-action" or "adding bill action" by check if there's any output on it already. 
			//will be considered as non-empty as long as there's output connecting to the id, even the output is not currently displaying on this bill.
			StringBuilder sql = new StringBuilder("select * from output where category = ").append(billID)
					.append(" or (subject = '").append(BarFrame.instance.cmbCurTable.getSelectedItem()).append("'")
					.append(" and time = '").append(BarFrame.instance.valStartTime.getText()).append("'")
					.append(" and contactID = ").append(curBill).append(")"); 	//in future version, might need to check the deleted property.
			ResultSet rs = PIMDBModel.getReadOnlyStatement().executeQuery(sql.toString());
			rs.afterLast();
		    rs.relative(-1);
		    
		    if(rs.getRow() == 0) {			//if still empty 
		    	//no related output, then don't record this bill in db at all.
		    	sql = new StringBuilder("delete from bill where id = ").append(billID);
		    	PIMDBModel.getStatement().executeUpdate(sql.toString());
		    	
		    } else { 						//if already has output.
		        //check if bill is already closed.
		        if(billPanel.status >= DBConsts.completed || billPanel.status < DBConsts.original) {
		        	JOptionPane.showMessageDialog(this, BarFrame.consts.ClosedBillCantVoid());
		        	return;
		        }
		        
		        //check if there's already send dish, give different warning message and remove them from panel.
		        if(billPanel.getNewDishes().size() < dishLength) {	//not all new
		    		if(JOptionPane.showConfirmDialog(BarFrame.instance, 
		    				BarFrame.consts.COMFIRMDELETEACTION(), DlgConst.DlgTitle, JOptionPane.YES_NO_OPTION) != 0) {
		                 return;	
		            }
		    		//if it's voiding a check printed bill, then we will regenerat a bill base on it, and set the regenerated bill as printed instead of original.
		    		if(billPanel.status >= DBConsts.billPrinted || billPanel.status < DBConsts.original) {
			    		if(!billPanel.checkStatus()) {	//this will regenerate a bill, but the new generated bill is original status. status will be unnecessarily checked again in
			    			return;						//method checkStatus(), but it dosn't harm, just some cpu time, so let it check again.
			    		}else {	//@NOTE if a new bill created, we want to set the status to be printed, so when it's print bill later, it will not send cancel info to kitchen.
					    	billPanel.status = DBConsts.billPrinted; //temperally set should be OK, because later in this method, will set status to void.
			    		}
		    		}
		        }else {												//all new
		        	if(JOptionPane.showConfirmDialog(BarFrame.instance, 
		    				BarFrame.consts.COMFIRMLOSTACTION(), DlgConst.DlgTitle, JOptionPane.YES_NO_OPTION) != 0) {
		                 return;	
		            }
		        }
		        
		        //clean the unsend items from billPanel first.
				for(int i = billPanel.orderedDishAry.size() - 1; i >= 0; i--) {
		    		if(billPanel.orderedDishAry.get(i).getOutputID() <= 0) {
		    			billPanel.orderedDishAry.remove(i);
		        	}
				}

		        //then mark all dishes which already send
		    	for (Dish dish : billPanel.orderedDishAry) {
		    		dish.setCanceled(true);	// to make it printed in special format(so it's know as a cancelled dish)
				}
		    	
		    	//print a final receipt or notice kitchen to stop preparing.
		    	if(billPanel.status >= DBConsts.billPrinted || billPanel.status < DBConsts.original) {		//if bill printed, print a refund bill.
		    		PrintService.exePrintVoid(billPanel);
		    	}else if(billPanel.orderedDishAry.size() > 0) { 	//otherwise, tell kitchen to stop preparing.
		    		billPanel.sendDishesToKitchen(billPanel.orderedDishAry, true);
		    	}
		    	
		    	//@NOTE: we need to process cur bill, give it a special status, so we can see the voided bills in check order dialog. 
		    	//and have to process it to be not null, better will not be considered as there's still non closed bill, when checking in isLastBill()
		    	//update bill
				sql = new StringBuilder("update bill set status = ").append(DBConsts.voided)
						.append(" where billIndex = '").append(curBill).append("'")
						.append(" and openTime = '").append(BarFrame.instance.valStartTime.getText()).append("'");
		    	PIMDBModel.getStatement().executeQuery(sql.toString());
		    	//update output
		    	sql = new StringBuilder("update output set deleted = ").append(DBConsts.voided)
		    			.append(" where contactID = ").append(curBill)
		    			.append(" and time = '").append(BarFrame.instance.valStartTime.getText()).append("'");
		        PIMDBModel.getStatement().executeQuery(sql.toString());
		        
		    }
		}catch(Exception exp) {
			L.e("void order", "error happend when voiding a bill with ID:"+ billID, exp);
		}
		
		if(BarOption.isFastFoodMode()) {
			String tableName = BarFrame.instance.cmbCurTable.getSelectedItem().toString();
			String newOpenTime = BarOption.df.format(new Date());
			int newBillIdx = BillListPanel.getANewBillIdx(tableName, newOpenTime);
			BarFrame.instance.valStartTime.setText(newOpenTime);
			BarFrame.instance.setCurBillIdx(String.valueOf(newBillIdx));
			
			billPanel.setBillID(BarFrame.instance.createAnEmptyBill(tableName, newOpenTime, newBillIdx));
			billPanel.initContent();
			
		}else {
			//if the bill amount is 1, cancel the selected status of the table.
			if(BarFrame.instance.isTableEmpty(null, null)) {
				BarFrame.instance.closeATable(null, null);
			}
			BarFrame.instance.switchMode(0);
		}
	}

	public void discountBill(float discount) {
		if(!billPanel.checkStatus()) {
			return;
		}
		billPanel.discount = discount > billPanel.subTotal ? billPanel.subTotal : discount;
		billPanel.updateTotleArea();
		
		createAndPrintNewOutput();
		billPricesUpdateToDB();
	}

	//add new bill with a new billID and billIdx.
	public void addNewBillInCurTable() {
		String tableName = BarFrame.instance.cmbCurTable.getSelectedItem().toString();
		String openTime = BarFrame.instance.valStartTime.getText();
		
		int newBillIdx = BillListPanel.getANewBillIdx(null, null);
		int oldbill = billPanel.getBillID();
		int billId = BarFrame.instance.createAnEmptyBill(tableName, openTime, newBillIdx);
		billPanel.setBillID(billId);
		BarFrame.instance.setCurBillIdx(String.valueOf(newBillIdx));
		BarFrame.instance.switchMode(2);
	}

	//Todo: Maybe it's safe to delete this method, because I think no need to touch ouputs.
//	private void reopenOutput() {
//		//convert the status of relevant output.
//		StringBuilder sql = new StringBuilder("update output set deleted = ").append(DBConsts.original)
//				.append(" where deleted = ").append(DBConsts.voided)
//				.append(" and category = ").append(billPanel.billID);
//		try {
//			PIMDBModel.getReadOnlyStatement().executeQuery(sql.toString());
//		}catch(Exception exp) {
//			L.e("SalesPane", "Exception happenned when converting output's status to 0", exp);
//		}
//	}

	public void showPriceChangeDlg() {
		BarFrame.numberPanelDlg.setTitle(BarFrame.consts.CHANGEPRICE());
		BarFrame.numberPanelDlg.setNotice(BarFrame.consts.ChangePriceNotice());
		BarFrame.numberPanelDlg.setBtnSource(btnChangePrice);//pomp up a numberPanelDlg
		BarFrame.numberPanelDlg.setFloatSupport(true);
		BarFrame.numberPanelDlg.setPercentSupport(false);
		BarFrame.numberPanelDlg.setModal(false);
		//should no record selected, select the last one.
		btnChangePrice.setSelected(true);
		BarFrame.numberPanelDlg.setVisible(btnChangePrice.isSelected());	//@NOTE: it's not model mode.
		BarFrame.numberPanelDlg.setAction(new UpdateItemPriceAction(btnChangePrice, billPanel));
	}

	//if there's new dish added.... update the total value field of bill record.
	//and make sure new added dish will be updated with new information.
	private void billPricesUpdateToDB() {
		BarUtil.updateBillRecordPrices(billPanel);		//in case if added service fee or discout of bill. ??? so this means, when added discount, will not save to db immediatly?
		billPanel.initContent();	//always need to initContent, to make sure dish in selection ary has new property. e.g. saved dish should has different color.,
	}

	public void createAndPrintNewOutput() {
		//if there's any new bill, send it to kitchen first, and this also made the output generated.
		List<Dish> dishes = billPanel.getNewDishes();
		if (dishes != null && dishes.size() > 0) {
			billPanel.sendNewOrdersToKitchenAndDB(dishes);
		}else {
			billPanel.sendNewOrdersToKitchenAndDB(BarUtil.generateAnEmptyDish());
 		}
	}

	public void discountADish(int value, Dish mostExpensiveDish) throws SQLException {
		if(!billPanel.checkStatus()) {
			return;
		}
		int outputID = mostExpensiveDish.getOutputID();
		if(outputID >= 0) {
			StringBuilder sql = new StringBuilder("update output set discount = ").append(value)
					.append(", toltalprice = ").append(Math.round(mostExpensiveDish.getTotalPrice() - value))
					.append(" where id = ").append(outputID);
			PIMDBModel.getStatement().executeUpdate(sql.toString());
		}
		
		billPanel.updateTotleArea();
		createAndPrintNewOutput();
		billPricesUpdateToDB();
	}
	
	void removeItem() {
		if(BillListPanel.curDish == null) {//check if there's an item selected.
			JOptionPane.showMessageDialog(this, BarFrame.consts.OnlyOneShouldBeSelected());
			return;
		}
		if(BillListPanel.curDish.getOutputID() >= 0) {//check if it's send
			if(JOptionPane.showConfirmDialog(BarFrame.instance, BarFrame.consts.COMFIRMDELETEACTION(), DlgConst.DlgTitle, JOptionPane.YES_NO_OPTION) != 0)
				return;
			//clean output from db.
			//No need to do it, will be called in method removeFromSelection() BillListPanel.curDish.changeOutputStatus(DBConsts.deleted);
			//send cancel message to kitchen
			BillListPanel.curDish.setCanceled(true);	//set the dish with cancelled flag, so when it's printout, will with "!!!!!".
			billPanel.sendDishToKitchen(BillListPanel.curDish, true);
			//clean from screen.
			billPanel.removeFromSelection(billPanel.table.getSelectedRow());
			//update bill info, must be after the screen update, because will get total from screen.
			BarUtil.updateBillRecordPrices(billPanel);
		}else {
			//only do clean from screen, because the output not generated yet, and will not affect the toltal in bill.
			billPanel.removeFromSelection(billPanel.table.getSelectedRow());
		}
	}

//    public static void resetCurTable(){
//    	try {
//            //clean all empty bill (match table id and opentime, status is null, while doesn't exist in any output.).
//            //if there's an output was deleted from this bill, this bill is still considered as empty.
//            //if there's an output was completed 10
////            sql = new StringBuilder("update bill set status = ").append(DBConsts.deleted)
////            		.append(" WHERE bill.id IN ( SELECT id FROM bill WHERE tableID = ").append(BarFrame.instance.valCurTable.getText())
////    				.append(" and OPENTIME = '").append(BarFrame.instance.valStartTime.getText())
////    				.append("' and status IS NULL OR status = ").append(DBConsts.original)
////    				.append(") AND NOT EXISTS (SELECT category FROM OUTPUT WHERE (deleted IS null or deleted = ").append(DBConsts.completed)
////    				.append(" AND time = '").append(BarFrame.instance.valStartTime.getText())
////    				.append("' and SUBJECT = '").append(BarFrame.instance.valCurTable.getText()).append("')");
//            
//            //no need to be complex, all ortiginal status bills of this table should be cleaned.
//            //close table
//            BarFrame.instance.closeATable(BarFrame.instance.cmbCurTable.getSelectedItem().toString(),
//            		BarFrame.instance.valStartTime.getText());
//    	}catch(Exception exp) {
//    		ErrorUtil.write(exp);
//    	}
//    }
    
    void reLayout() {
        int panelHeight = getHeight();

        int tBtnWidht = (getWidth() - CustOpts.HOR_GAP * 9) / 9;
        int tBtnHeight = panelHeight / 10;

        // command buttons--------------
        // line 2
        int y = panelHeight - tBtnHeight - CustOpts.VER_GAP;
        //btnAddBill.setBounds(btnReturn.getX() + tBtnWidht + CustOpts.HOR_GAP, btnReturn.getY(), tBtnWidht, tBtnHeight);
        btnMASTER.setBounds(CustOpts.HOR_GAP, y, tBtnWidht, tBtnHeight);
        btnOTHER.setBounds(btnMASTER.getX() + tBtnWidht + CustOpts.HOR_GAP, y, tBtnWidht, tBtnHeight);
        btnDiscountCoupon.setBounds(btnOTHER.getX() + tBtnWidht + CustOpts.HOR_GAP, y, tBtnWidht, tBtnHeight);
        btnCancelAll.setBounds(btnDiscountCoupon.getX() + tBtnWidht + CustOpts.HOR_GAP, y, tBtnWidht, tBtnHeight);
        //btnVoidOrder.setBounds(btnCancelAll.getX() + tBtnWidht + CustOpts.HOR_GAP, btnReturn.getY(), tBtnWidht, tBtnHeight);
        btnDiscBill.setBounds(btnCancelAll.getX() + tBtnWidht + CustOpts.HOR_GAP, y, tBtnWidht, tBtnHeight);
        btnRefund.setBounds(btnDiscBill.getX() + tBtnWidht + CustOpts.HOR_GAP, y, tBtnWidht, tBtnHeight);
        btnOpenDrawer.setBounds(btnRefund.getX() + tBtnWidht + CustOpts.HOR_GAP, y, tBtnWidht, tBtnHeight);
        //btnMore.setBounds(btnOpenDrawer.getX() + tBtnWidht + CustOpts.HOR_GAP, y, tBtnWidht, tBtnHeight);
        //btnSend.setBounds(btnMore.getX() + tBtnWidht + CustOpts.HOR_GAP, btnReturn.getY(), tBtnWidht, tBtnHeight);
        btnSetting.setBounds(btnOpenDrawer.getX() + tBtnWidht + CustOpts.HOR_GAP, y, tBtnWidht, tBtnHeight);
        btnReturn.setBounds(btnSetting.getX() + tBtnWidht + CustOpts.HOR_GAP, y, tBtnWidht, tBtnHeight);
        // line 1
        y -= tBtnHeight + CustOpts.VER_GAP;
        btnCASH.setBounds(btnMASTER.getX(),  y, tBtnWidht, tBtnHeight);
        btnDEBIT.setBounds(btnCASH.getX() + tBtnWidht + CustOpts.HOR_GAP, y, tBtnWidht, tBtnHeight);
        btnVISA.setBounds(btnDEBIT.getX() + tBtnWidht + CustOpts.HOR_GAP, y, tBtnWidht, tBtnHeight);
        //btnSplitBill.setBounds(btnCancelAll.getX(), y, tBtnWidht, tBtnHeight);
        btnRemoveItem.setBounds(btnVISA.getX() + tBtnWidht + CustOpts.HOR_GAP, y, tBtnWidht, tBtnHeight);
        //btnModify.setBounds(btnOpenDrawer.getX(), y, tBtnWidht, tBtnHeight);
        btnDiscItem.setBounds(btnRemoveItem.getX() + tBtnWidht + CustOpts.HOR_GAP, y, tBtnWidht, tBtnHeight);
        btnChangePrice.setBounds(btnDiscItem.getX() + tBtnWidht + CustOpts.HOR_GAP, y, tBtnWidht, tBtnHeight);
        btnSuspend.setBounds(btnChangePrice.getX() + tBtnWidht + CustOpts.HOR_GAP, y, tBtnWidht, tBtnHeight);
        btnCheckOrder.setBounds(btnSuspend.getX() + tBtnWidht + CustOpts.HOR_GAP, y, tBtnWidht, tBtnHeight);
        btnReport.setBounds(btnCheckOrder.getX() + tBtnWidht + CustOpts.HOR_GAP, y, tBtnWidht, tBtnHeight);
        //btnServiceFee.setBounds(btnMore.getX(), y, tBtnWidht, tBtnHeight);
        //btnPrintBill.setBounds(btnMore.getX(), y, tBtnWidht, tBtnHeight);

//        btnLine_2_11.setBounds(btnLine_2_5.getX() + tBtnWidht + CustOpts.HOR_GAP, btnLine_1_2.getY(), tBtnWidht, tBtnHeight);
//        btnLine_2_12.setBounds(btnLine_1_5.getX() + tBtnWidht + CustOpts.HOR_GAP, btnLine_2_14.getY(), tBtnWidht, tBtnHeight);
//        btnLine_2_13.setBounds(btnLine_1_4.getX() + tBtnWidht + CustOpts.HOR_GAP, btnLine_2_14.getY(), tBtnWidht, tBtnHeight);
//        btnLine_2_14.setBounds(CustOpts.HOR_GAP,, tBtnWidht, tBtnHeight);
        
        // TOP part============================
        int topAreaHeight = btnCASH.getY() - 3 * CustOpts.VER_GAP;

        billPanel.setBounds(CustOpts.HOR_GAP, CustOpts.VER_GAP,
                (int) (getWidth() * (1 - BarOption.getMenuAreaPortion())), topAreaHeight);
        
        // menu area--------------
        int xMenuArea = billPanel.getX() + billPanel.getWidth() + CustOpts.HOR_GAP;
        int widthMenuArea = getWidth() - billPanel.getWidth() - CustOpts.HOR_GAP * 2 - CustOpts.SIZE_EDGE;

        BarFrame.menuPanel.setBounds(xMenuArea, billPanel.getY(), widthMenuArea, topAreaHeight);
        BarFrame.menuPanel.reLayout();

        billPanel.resetColWidth(billPanel.getWidth());
    }

    void initComponent() {
    	removeAll();	//when it's called by setting panel(changed colors...), it will be called to refresh.
        btnCASH = new FunctionButton(BarFrame.consts.CASH());
        btnDEBIT = new FunctionButton(BarFrame.consts.DEBIT());
        btnVISA = new FunctionButton(BarFrame.consts.VISA());
        btnSplitBill = new FunctionButton(BarFrame.consts.SPLIT_BILL());
        btnRemoveItem = new FunctionButton(BarFrame.consts.REMOVEITEM());
        btnModify = new FunctionButton(BarFrame.consts.MODIFY());
        btnDiscItem = new JToggleButton(BarFrame.consts.DISC_ITEM());
        btnChangePrice = new JToggleButton(BarFrame.consts.ChangePrice());
        btnServiceFee = new FunctionButton(BarFrame.consts.SERVICEFEE());
        btnPrintBill = new FunctionButton(BarFrame.consts.PRINT_BILL());

        btnReturn = new FunctionButton(BarFrame.consts.RETURN());
        btnAddBill = new FunctionButton(BarFrame.consts.AddUser());
        btnMASTER = new FunctionButton(BarFrame.consts.MASTER());
        btnOTHER = new FunctionButton(BarFrame.consts.GIFTCARD());
        btnDiscountCoupon = new FunctionButton(BarFrame.consts.COUPON());
        btnCancelAll = new FunctionButton(BarFrame.consts.CANCEL_ALL());
        btnVoidOrder = new FunctionButton(BarFrame.consts.VOID_ORDER());
        btnOpenDrawer = new FunctionButton(BarFrame.consts.OpenDrawer());
        btnDiscBill = new FunctionButton(BarFrame.consts.VolumnDiscount());
        btnRefund = new FunctionButton(BarFrame.consts.Refund());
        btnMore = new FunctionButton(BarFrame.consts.MORE());
        btnSend = new FunctionButton(BarFrame.consts.SEND());
        
        btnSetting = new FunctionButton(BarFrame.consts.SETTINGS());
        btnSuspend = new FunctionButton(BarFrame.consts.SUSPEND());
        btnCheckOrder = new FunctionButton(BarFrame.consts.OrderManage());
        btnReport = new FunctionButton(BarFrame.consts.Report());
        
        billPanel = new BillPanel(this);
        // properties
        Color bg = BarOption.getBK("Sales");
    	if(bg == null) {
    		bg = new Color(216,216,216);
    	}
		setBackground(bg);
        setLayout(null);
        
        // built
        add(btnCASH);
        add(btnDEBIT);
        add(btnVISA);
        add(btnSplitBill);
        add(btnRemoveItem);
        add(btnModify);
        add(btnDiscItem);
        add(btnChangePrice);
        add(btnServiceFee);
        add(btnPrintBill);
        add(btnSuspend);
        add(btnCheckOrder);
        add(btnReport);

        add(btnReturn);
        add(btnAddBill);
        add(btnMASTER);
        add(btnOTHER);
        add(btnDiscountCoupon);
        add(btnCancelAll);
        add(btnVoidOrder);
        add(btnOpenDrawer);
        add(btnDiscBill);
        add(btnRefund);
        add(btnMore);
        add(btnSend);
        add(btnSetting);
        
        add(billPanel);
        // add listener
        addComponentListener(this);

        // 因为考虑到条码经常由扫描仪输入，不一定是靠键盘，所以专门为他加了DocumentListener，通过监视内容变化来自动识别输入完成，光标跳转。
        // tfdProdNumber.getDocument().addDocumentListener(this); // 而其它组件如实收金额框不这样做为了节约（一个KeyListener接口全搞定）

        btnCASH.addActionListener(this);
        btnDEBIT.addActionListener(this);
        btnVISA.addActionListener(this);
        btnSplitBill.addActionListener(this);
        btnRemoveItem.addActionListener(this);
        btnModify.addActionListener(this);
        btnDiscItem.addActionListener(this);
        btnChangePrice.addActionListener(this);
        btnServiceFee.addActionListener(this);
        btnPrintBill.addActionListener(this);
        btnSuspend.addActionListener(this);
        btnCheckOrder.addActionListener(this);
        btnReport.addActionListener(this);

        btnReturn.addActionListener(this);
        btnAddBill.addActionListener(this);
        btnMASTER.addActionListener(this);
        btnCancelAll.addActionListener(this);
        btnVoidOrder.addActionListener(this);
        btnOpenDrawer.addActionListener(this);
        btnDiscBill.addActionListener(this);
        btnRefund.addActionListener(this);
        btnMore.addActionListener(this);
        btnSend.addActionListener(this);
        btnSetting.addActionListener(this);
        btnOTHER.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String giftCardNumber  = JOptionPane.showInputDialog(null, BarFrame.consts.Account());
	    		if(giftCardNumber == null || giftCardNumber.length() == 0)
	    			return;
	    		
	    		StringBuilder sql = new StringBuilder("SELECT * from hardware where category = 2 and name = '").append(giftCardNumber)
	    				.append("' and (status is null or status = 0)");
	    		try {
	    			ResultSet rs = PIMDBModel.getReadOnlyStatement().executeQuery(sql.toString());
	    			rs.afterLast();
	                rs.relative(-1);
	                int tmpPos = rs.getRow();
	                if(tmpPos == 0) {	//if there's no this coupon number in database, then warning and return.
	                	JOptionPane.showMessageDialog(null, BarFrame.consts.InvalidCoupon());
	                	return;
	                }else {			//if the number is OK.
	                	//get out every field of first matching record.
	                	rs.beforeFirst();
	                    tmpPos = 0;
	                    rs.next();
	                    int id = rs.getInt("id");
	                    int category = rs.getInt("style");
	                    String productCode = rs.getString("IP");
	                    int value = rs.getInt("langType");
	                    
	                    //show up the payDialog, waiting for user to input money, after confirm, the money should be deduct from the account of this card
	                    SalesPanel salesPanel = (SalesPanel)BarFrame.instance.panels[2];
	                    BarFrame.payDlg.maxInput = (float)(value / 100.0);
	                    BarFrame.setStatusMes(BarFrame.consts.CurrentBalanceMsg() + BarFrame.payDlg.maxInput);
	                    salesPanel.actionPerformed(new ActionEvent(salesPanel.btnOTHER, 0, ""));
	                    //how to know the number user inputed, and how to verify if it's bigger than the money left in card?
	                    if (BarFrame.payDlg.inputedContent != null && BarFrame.payDlg.inputedContent.length() > 0) {
		                    float usedMoneyQT = Math.round(Float.valueOf(BarFrame.payDlg.inputedContent) * 100);
		                    sql = new StringBuilder("update hardware set langType = langType - ").append(usedMoneyQT)
		                    		.append(" where id = ").append(id);
		                    PIMDBModel.getStatement().executeUpdate(sql.toString());
	                    }
	                }
	    		}catch(Exception exp) {
	    			L.e("Redeem Coupon", "exception happend when redeem coupon: " + sql, exp);
	    		}
			}
		});
        btnDiscountCoupon.addActionListener(this);
		reLayout();
    }

	private ArrayList<String> getAppliableDishNames(String productCode, int langIdx) {
    	ArrayList<String> appliableDishes = new ArrayList<String>();
    	if(productCode == null || productCode.trim().length() == 0) {
    		return null;
    	}
    	//if it's a category
    	String[] codes = productCode.split(",");
        Dish[] dishAry = BarFrame.menuPanel.getDishAry();
    	for(int m = 0; m < codes.length; m++) {
			if(codes[m].length() == 0) {
				continue;
			}
			boolean matched = false;
	    	for(int i = 0; i < BarFrame.menuPanel.categoryNameMetrix[0].length; i++) {
	    		//check 3 languages
    			if(codes[m].trim().equalsIgnoreCase(BarFrame.menuPanel.categoryNameMetrix[0][i].trim())
    					|| codes[m].trim().equalsIgnoreCase(BarFrame.menuPanel.categoryNameMetrix[1][i].trim())
    					|| codes[m].trim().equalsIgnoreCase(BarFrame.menuPanel.categoryNameMetrix[2][i].trim())) {
    				//add all relavant dishes
    		        for (int j = 0; j < dishAry.length; j++) {
    					if(dishAry[j].getCATEGORY().trim().equals(BarFrame.menuPanel.categoryNameMetrix[0][i].trim())) {
    						appliableDishes.add(dishAry[j].getLanguage(langIdx));
    					}
    		        }
    				matched = true;
    				break;
    			}
	    	}
	    	
	    	//didn't match any category, then it's a menu name, add the lang0 into the list directly.
	    	if(!matched) {
	    		for (int j = 0; j < dishAry.length; j++) {
					if(codes[m].trim().equalsIgnoreCase(dishAry[j].getLanguage(0).trim())
						|| codes[m].trim().equalsIgnoreCase(dishAry[j].getLanguage(1).trim())
						|| codes[m].trim().equalsIgnoreCase(dishAry[j].getLanguage(2).trim())){
						appliableDishes.add(dishAry[j].getLanguage(langIdx));
					}
		        }
	    	}
    	}
		return appliableDishes;
	}
	
    //find out which dishes in current bill can be apply on the coupon.
    private ArrayList<Dish> getMatchedItem(ArrayList<Dish> dishesOnBill, ArrayList<String> nameAry, int langIdx) {
    	if(nameAry == null) {
    		return null;
    	}
    	ArrayList<Dish> appliableDishes = new ArrayList<Dish>();
    	for (Dish dish : dishesOnBill) {
			if(nameAry.contains(dish.getLanguage(langIdx))) {
				appliableDishes.add(dish);
			}
		}
		return appliableDishes;
	}
    
    private FunctionButton btnCASH;
    private FunctionButton btnDEBIT;
    private FunctionButton btnVISA;
    private FunctionButton btnSplitBill;
    private FunctionButton btnRemoveItem;
    private FunctionButton btnModify;
    public JToggleButton btnDiscItem;
    JToggleButton btnChangePrice;
    private FunctionButton btnServiceFee;
    private FunctionButton btnPrintBill;

    private FunctionButton btnReturn;
    private FunctionButton btnAddBill;
    private FunctionButton btnMASTER;
    public FunctionButton btnOTHER;
    private FunctionButton btnCancelAll;
    private FunctionButton btnVoidOrder;
    private FunctionButton btnOpenDrawer;
    private FunctionButton btnDiscBill;
    private FunctionButton btnRefund;
    private FunctionButton btnMore;
    private FunctionButton btnSend;
    
    //from more button dlg.
	private FunctionButton btnReport;
	private FunctionButton btnDiscountCoupon;
	private FunctionButton btnSetting;
	private FunctionButton btnSuspend;
	private FunctionButton btnCheckOrder;
	
    public BillPanel billPanel;
}
