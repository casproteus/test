package org.cas.client.platform.bar.dialog.statistics;

import java.awt.Color;
import java.awt.Container;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.cas.client.platform.bar.BarUtil;
import org.cas.client.platform.bar.dialog.BarFrame;
import org.cas.client.platform.bar.dialog.BarOption;
import org.cas.client.platform.bar.dialog.SalesPanel;
import org.cas.client.platform.bar.model.DBConsts;
import org.cas.client.platform.casbeans.textpane.PIMTextPane;
import org.cas.client.platform.cascontrol.dialog.ICASDialog;
import org.cas.client.platform.cascontrol.dialog.logindlg.LoginDlg;
import org.cas.client.platform.cascustomize.CustOpts;
import org.cas.client.platform.casutil.ErrorUtil;
import org.cas.client.platform.pimmodel.PIMDBModel;
import org.cas.client.platform.pimmodel.PIMRecord;
import org.cas.client.platform.pimview.pimscrollpane.PIMScrollPane;
import org.cas.client.platform.pimview.pimtable.DefaultPIMTableCellRenderer;
import org.cas.client.platform.pimview.pimtable.IPIMTableColumnModel;
import org.cas.client.platform.pimview.pimtable.PIMTable;

public class CheckRuleDlg extends JDialog implements ICASDialog, ActionListener, ComponentListener, KeyListener, ListSelectionListener {
    
	private boolean isEditingBillID;
	
    /**
     * Creates a new instance of ContactDialog
     * 
     * @called by PasteAction 为Copy邮件到联系人应用。
     */
    public CheckRuleDlg(JFrame pParent) {
        super(pParent, true);
        initDialog();
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(
            KeyEvent e) {
        Object o = e.getSource();
        switch (e.getKeyCode()) {
            case 37:
                tblContent.scrollToRect(tblContent.getSelectedRow(), tblContent.getSelectedColumn() - 1);
                break;
            case 38:
                tblContent.setSelectedRow(tblContent.getSelectedRow() - 1);
                tblContent.scrollToRect(tblContent.getSelectedRow(), tblContent.getSelectedColumn());
                break;
            case 39:
                tblContent.scrollToRect(tblContent.getSelectedRow(), tblContent.getSelectedColumn() + 1);
                break;
            case 40:
                tblContent.setSelectedRow(tblContent.getSelectedRow() + 1);
                tblContent.scrollToRect(tblContent.getSelectedRow(), tblContent.getSelectedColumn());
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    /*
     * 对话盒的布局独立出来，为了在对话盒尺寸发生改变后，界面各元素能够重新布局， 使整体保持美观。尤其在Linux系列的操作系统上，所有的对话盒都必须准备好应对用户的拖拉改变尺寸。
     * @NOTE:因为setBounds方法本身不会触发事件导致重新布局，所以本方法中设置Bounds之后调用了reLayout。
     */
    @Override
    public void reLayout() {
        srpContent.setBounds(CustOpts.HOR_GAP, CustOpts.VER_GAP, 
        		getWidth() - CustOpts.SIZE_EDGE * 2 - CustOpts.HOR_GAP * 2, 
                getHeight() - CustOpts.SIZE_TITLE - CustOpts.SIZE_EDGE - CustOpts.VER_GAP * 3
                - CustOpts.BTN_WIDTH_NUM);
        
        int btnWidth = 80;
        btnChangeDate.setBounds(getWidth() - CustOpts.SIZE_EDGE - CustOpts.HOR_GAP * 2 - btnWidth * 2, srpContent.getY() + srpContent.getHeight() + CustOpts.VER_GAP,
                btnWidth, CustOpts.BTN_WIDTH_NUM);
        btnPrintInvoice.setBounds(btnChangeDate.getX() + btnChangeDate.getWidth() + CustOpts.HOR_GAP, btnChangeDate.getY(),
                btnWidth, CustOpts.BTN_WIDTH_NUM);
        
        IPIMTableColumnModel tTCM = tblContent.getColumnModel();
        tTCM.getColumn(0).setPreferredWidth(130);	//BarFrame.consts.TIME
        tTCM.getColumn(1).setPreferredWidth(40);	//BarFrame.consts.Table,
        tTCM.getColumn(2).setPreferredWidth(40);	//BarFrame.consts.Bill
        tTCM.getColumn(3).setPreferredWidth(60);	//BarFrame.consts.Total
        tTCM.getColumn(4).setPreferredWidth(60);	//BarFrame.consts.Discount
        tTCM.getColumn(5).setPreferredWidth(60);	//BarFrame.consts.Receive
        tTCM.getColumn(6).setPreferredWidth(60);	//BarFrame.consts.Tip
        tTCM.getColumn(7).setPreferredWidth(60);	//BarFrame.consts.CashBack
        tTCM.getColumn(8).setPreferredWidth(60);	//BarFrame.consts.Status
        tTCM.getColumn(9).setPreferredWidth(60);	//BarFrame.consts.Operator
        tTCM.getColumn(10).setPreferredWidth(srpContent.getWidth() - 635);	//BarFrame.consts.comment
        tTCM.getColumn(11).setPreferredWidth(0);	//table open time, invisible.
        tTCM.getColumn(12).setPreferredWidth(60);	//cash received.
        tTCM.getColumn(13).setPreferredWidth(60);	//debit received..
        tTCM.getColumn(14).setPreferredWidth(60);	//visa received..
        tTCM.getColumn(15).setPreferredWidth(60);	//master received..
        tTCM.getColumn(16).setPreferredWidth(60);	//otehr received.
        tTCM.getColumn(17).setPreferredWidth(40);	//id.
        validate();
    }

    @Override
    public PIMRecord getContents() {return null;}

    @Override
    public boolean setContents( PIMRecord prmRecord) {return true;}

    @Override
    public void makeBestUseOfTime() {}

    @Override
    public void addAttach(File[] file, Vector actualAttachFiles) {}

    @Override
    public PIMTextPane getTextPane() {return null;}

    @Override
    public void release() {
        btnChangeDate.removeActionListener(this);
        btnPrintInvoice.removeActionListener(this);
        dispose();// 对于对话盒，如果不加这句话，就很难释放掉。
        System.gc();// @TODO:不能允许私自运行gc，应该改为象收邮件线程那样低优先级地自动后台执行，可以从任意方法设置立即执行。
    }

    @Override
    public void componentResized(ComponentEvent e) {
        reLayout();
    };

    @Override
    public void componentMoved(ComponentEvent e) {};

    @Override
    public void componentShown(ComponentEvent e) {};

    @Override
    public void componentHidden(ComponentEvent e) {};

    @Override
    public void actionPerformed(
            ActionEvent e) {
        Object o = e.getSource();
        if (o == btnChangeDate) {
        	reLayout();
        }else if(o == btnPrintInvoice) {
        }
    }
	
	@Override
	public void valueChanged(ListSelectionEvent e) {
		showInSalesPanel();
	}
	
    private boolean showInSalesPanel() {
    	int selectedRow = tblContent.getSelectedRow();
    	int tValidRowCount = getUsedRowCount();
    	if(selectedRow < 0 || selectedRow > tValidRowCount - 1) {
    		JOptionPane.showMessageDialog(this, BarFrame.consts.OnlyOneShouldBeSelected());
    		ErrorUtil.write("Unexpected row number when calling removeAtSelection : " + selectedRow);
    		return false;
    	}
    	//swith to sales panel.

        BarFrame.instance.ignoreItemChange = true;
    	BarFrame.instance.cmbCurTable.setSelectedItem(String.valueOf(tblContent.getValueAt(selectedRow, 1)));
    	BarFrame.instance.setCurBillIdx(String.valueOf(tblContent.getValueAt(selectedRow, 2)));
    	BarFrame.instance.valOperator.setText(String.valueOf(tblContent.getValueAt(selectedRow, 9)));
    	BarFrame.instance.valStartTime.setText(String.valueOf(tblContent.getValueAt(selectedRow, 11)));

    	((SalesPanel)BarFrame.instance.panels[2]).billPanel.setBillID(Integer.valueOf(String.valueOf(tblContent.getValueAt(selectedRow, 17))));
    	
    	//if this flag set, the initContent will choose outputs and bill differently.
    	//NOTE: there's could be one final and several expired bills under same tableid and billIdx and opentime. we don't support more than one exipred bill.
    	BarFrame.instance.isShowingAnExpiredBill = "expired".equals(tblContent.getValueAt(selectedRow, 8));
    	BarFrame.instance.setCurBillID(Integer.valueOf(String.valueOf(tblContent.getValueAt(selectedRow, 17))));
    	BarFrame.instance.switchMode(2);
    	return true;
    }
    
    private int getUsedRowCount() {
        for (int i = 0, len = tblContent.getRowCount(); i < len; i++)
            if (tblContent.getValueAt(i, 0) == null)
                return i; // 至此得到 the used RowCount。
        return tblContent.getRowCount();
    }
    
    @Override
    public Container getContainer() {
        return getContentPane();
    }

    private void initDialog() {
        setTitle(BarFrame.consts.SaleRecs());
        setModal(false);

        // 初始化－－－－－－－－－－－－－－－－
        tblContent = new PIMTable();// 显示字段的表格,设置模型
        srpContent = new PIMScrollPane(tblContent);

        btnChangeDate = new JButton(BarFrame.consts.Apply());
        btnPrintInvoice = new JButton(BarFrame.consts.PRINT());
        // properties
        btnChangeDate.setMnemonic('A');
        btnChangeDate.setMargin(new Insets(0, 0, 0, 0));
        btnPrintInvoice.setMnemonic('P');
        btnPrintInvoice.setMargin(new Insets(0, 0, 0, 0));
        
        tblContent.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblContent.setAutoscrolls(true);
        tblContent.setRowHeight(30);
        tblContent.setBorder(new JTextField().getBorder());
        tblContent.setFocusable(false);

        JLabel tLbl = new JLabel();
        tLbl.setOpaque(true);
        tLbl.setBackground(Color.GRAY);
        srpContent.setCorner(JScrollPane.LOWER_RIGHT_CORNER, tLbl);

        // 布局---------------
        setBounds((CustOpts.SCRWIDTH - 780) / 2, (CustOpts.SCRHEIGHT - 500) / 2, 780, 500); // 对话框的默认尺寸。
        getContentPane().setLayout(null);

        // 搭建－－－－－－－－－－－－－
        getContentPane().add(srpContent);
        
        getContentPane().add(btnChangeDate);
        getContentPane().add(btnPrintInvoice);
        // 加监听器－－－－－－－－
        tblContent.getSelectionModel().addListSelectionListener(this);
        
        btnChangeDate.addActionListener(this);
        btnChangeDate.addKeyListener(this);
        btnPrintInvoice.addActionListener(this);
        btnPrintInvoice.addKeyListener(this);
        getContentPane().addComponentListener(this);
    }

    public void initContent(String startTime, String endTime) {
        
        StringBuilder sql = new StringBuilder("select * from bill, employee where createTime >= '").append(startTime)
        		.append("' and createTime <= '").append(endTime)
        		.append("' and bill.employeeId = employee.id and (bill.status < ").append(LoginDlg.USERTYPE < 2 ? DBConsts.expired : DBConsts.deleted)
        		.append(" or bill.status is null)");
        //if didn't set to ShowProcessingBill then show only completed(paid) bill
        if(!"true".equalsIgnoreCase(String.valueOf(CustOpts.custOps.getValue("ShowProcessingBill")))) {
        	sql.append(" and bill.status is not null ").append(" and bill.status != ").append(DBConsts.billPrinted);
        }
        //if configured, then do not show records of other waiter.
        if("true".equalsIgnoreCase(String.valueOf(CustOpts.custOps.getValue("HideRecordFromOtherWaiter"))) 
        		&& LoginDlg.USERTYPE < 2) {
        	sql.append(" and employee.id = ").append(LoginDlg.USERID);
        }
        
        sql.append(" order by createTime desc");
        fillTableAreaWithResultSet(sql);
        
    }
    
    public void initContent() {
        StringBuilder sql = new StringBuilder("select * from CustomizedRule where status != -100");
        fillTableAreaWithResultSet(sql);
        reLayout();
    }

	private void fillTableAreaWithResultSet(StringBuilder sql) {
		Object[][] tValues = null;
		try {
            ResultSet rs = PIMDBModel.getReadOnlyStatement().executeQuery(sql.toString());
            rs.afterLast();
            rs.relative(-1);
            int tmpPos = rs.getRow();
            tValues = new Object[tmpPos][header.length];
            rs.beforeFirst();
            tmpPos = 0;
            while (rs.next()) {
                tValues[tmpPos][0] = rs.getString("createTime");;
                tValues[tmpPos][1] = rs.getString("tableID");
                tValues[tmpPos][2] = rs.getString("billIndex");
                tValues[tmpPos][3] = BarUtil.formatMoney(rs.getInt("total") / 100.0);
                tValues[tmpPos][4] = BarUtil.formatMoney(rs.getInt("discount") / 100.0);
                int received = rs.getInt("cashReceived") + rs.getInt("debitReceived")
                + rs.getInt("visaReceived") + rs.getInt("masterReceived") + rs.getInt("otherreceived"); 
                tValues[tmpPos][5] = BarUtil.formatMoney(received / 100.0);
                tValues[tmpPos][6] = BarUtil.formatMoney(rs.getInt("tip") / 100.0);
                tValues[tmpPos][7] = BarUtil.formatMoney(rs.getInt("cashback") / 100.0);
                int status = rs.getInt("status");
                switch (status) {
				case DBConsts.original:
					tValues[tmpPos][8] = "to pay";
					break;
				case DBConsts.billPrinted:
					tValues[tmpPos][8] = "billed";
					break;
				case DBConsts.suspended:
					tValues[tmpPos][8] = "suspended";
					break;
				case DBConsts.completed:
					tValues[tmpPos][8] = "paid";
					break;
				case DBConsts.voided:
					tValues[tmpPos][8] = "void";
					break;
				case DBConsts.expired:
					tValues[tmpPos][8] = "expired";
					break;
				default:
					tValues[tmpPos][8] = BarUtil.formatMoney(status / 100.0);
					break;
				}
                tValues[tmpPos][9] = rs.getString("employee.nName");
                tValues[tmpPos][10] = rs.getString("Comment");
                tValues[tmpPos][11] = rs.getString("OpenTime");
                tValues[tmpPos][12] = BarUtil.formatMoney(rs.getInt("cashReceived") / 100.0);
                tValues[tmpPos][13] = BarUtil.formatMoney(rs.getInt("debitReceived") / 100.0);
                tValues[tmpPos][14] = BarUtil.formatMoney(rs.getInt("visaReceived") / 100.0);
                tValues[tmpPos][15] = BarUtil.formatMoney(rs.getInt("masterReceived") / 100.0);
                tValues[tmpPos][16] = BarUtil.formatMoney(rs.getInt("otherreceived") / 100.0);
                tValues[tmpPos][17] = rs.getString("id");
                
                tmpPos++;
            }
            rs.close();// 关闭
        } catch (SQLException e) {
            ErrorUtil.write(e);
        }

        tblContent.setDataVector(tValues, header);
        DefaultPIMTableCellRenderer tCellRender = new DefaultPIMTableCellRenderer();
        tCellRender.setOpaque(true);
        tCellRender.setBackground(Color.LIGHT_GRAY);
        tblContent.getColumnModel().getColumn(1).setCellRenderer(tCellRender);
	}
    
    private void initPordAndEmploy() {
        String sql = "select ID, UserName from UserIdentity";
        try {
            ResultSet rs = PIMDBModel.getReadOnlyStatement().executeQuery(sql);
            rs.afterLast();
            rs.relative(-1);
            int tmpPos = rs.getRow();
            employIdAry = new int[tmpPos + 1];
            employNameAry = new String[tmpPos + 1];
            rs.beforeFirst();

            tmpPos = 0;
            while (rs.next()) {
                employIdAry[tmpPos] = rs.getInt("ID");
                employNameAry[tmpPos] = rs.getString("UserName");
                tmpPos++;
            }
            employIdAry[tmpPos] = -1;
            employNameAry[tmpPos] = "-";
            rs.close();// 关闭
        } catch (SQLException e) {
            ErrorUtil.write(e);
        }

        // for the service.
        sql = "select ID, SUBJECT from Product";
        try {
            ResultSet rs = PIMDBModel.getReadOnlyStatement().executeQuery(sql);
            rs.afterLast();
            rs.relative(-1);
            int tmpPos = rs.getRow();
            prodIdAry = new int[tmpPos];
            prodNameAry = new String[tmpPos];
            rs.beforeFirst();

            tmpPos = 0;
            while (rs.next()) {
                prodIdAry[tmpPos] = rs.getInt("ID");
                prodNameAry[tmpPos] = rs.getString("SUBJECT");
                tmpPos++;
            }
            rs.close();// 关闭
        } catch (SQLException e) {
            ErrorUtil.write(e);
        }
    }

    private String[] header = new String[] {
    		BarFrame.consts.TIME(), // "时间"
    		BarFrame.consts.TABLE(),
    		BarFrame.consts.Bill(),
    		BarFrame.consts.Total(),
    		BarFrame.consts.Discount(),
    		BarFrame.consts.Receive(),
    		BarFrame.consts.Tip(),
    		BarFrame.consts.Changes(),
    		BarFrame.consts.Status(),
    		BarFrame.consts.Operator(), // "操作员"
    		BarFrame.consts.comment(),
    		BarFrame.consts.OpenTime(),
    		BarFrame.consts.CASH(),
    		BarFrame.consts.DEBIT(),
    		BarFrame.consts.VISA(),
    		BarFrame.consts.MASTER(),
    		BarFrame.consts.GIFTCARD(),
    		BarFrame.consts.BILL()
    };

    int[] employIdAry;
    String[] employNameAry;
    int[] prodIdAry;
    String[] prodNameAry;
    
    PIMTable tblContent;
    PIMScrollPane srpContent;

    private JButton btnChangeDate;
    private JButton btnPrintInvoice;
}
