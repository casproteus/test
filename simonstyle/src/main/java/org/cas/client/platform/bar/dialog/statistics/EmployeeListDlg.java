package org.cas.client.platform.bar.dialog.statistics;

import java.awt.Color;
import java.awt.Container;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.cas.client.platform.CASControl;
import org.cas.client.platform.bar.dialog.BarFrame;
import org.cas.client.platform.casbeans.textpane.PIMTextPane;
import org.cas.client.platform.cascontrol.dialog.ICASDialog;
import org.cas.client.platform.cascontrol.dialog.logindlg.LoginDlg;
import org.cas.client.platform.cascontrol.menuaction.SaveContentsAction;
import org.cas.client.platform.cascontrol.menuaction.UpdateContactAction;
import org.cas.client.platform.cascustomize.CustOpts;
import org.cas.client.platform.casutil.ErrorUtil;
import org.cas.client.platform.casutil.PIMPool;
import org.cas.client.platform.contact.ContactDefaultViews;
import org.cas.client.platform.employee.EmployeeDefaultViews;
import org.cas.client.platform.employee.dialog.EmployeeDlg;
import org.cas.client.platform.pimmodel.PIMDBModel;
import org.cas.client.platform.pimmodel.PIMRecord;
import org.cas.client.platform.pimview.pimscrollpane.PIMScrollPane;
import org.cas.client.platform.pimview.pimtable.DefaultPIMTableCellRenderer;
import org.cas.client.platform.pimview.pimtable.IPIMTableColumnModel;
import org.cas.client.platform.pimview.pimtable.PIMTable;
import org.cas.client.platform.pos.dialog.PosFrame;

public class EmployeeListDlg extends JDialog implements ICASDialog, ActionListener, ComponentListener, KeyListener,
        MouseListener {
    final int IDCOLUM = 0; //with column id field stays.

    private Object[][] tableModel = null;
    
    /**
     * Creates a new instance of ContactDialog
     * 
     * @called by PasteAction 为Copy邮件到联系人应用。
     */
    public EmployeeListDlg(JFrame pParent) {
        super(pParent, true);
        initDialog();
    }

    @Override
    public void keyTyped(
            KeyEvent e) {
    }

    @Override
    public void keyPressed(
            KeyEvent e) {
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
    public void keyReleased(
            KeyEvent e) {
    }

    /*
     * 对话盒的布局独立出来，为了在对话盒尺寸发生改变后，界面各元素能够重新布局， 使整体保持美观。尤其在Linux系列的操作系统上，所有的对话盒都必须准备好应对用户的拖拉改变尺寸。
     * @NOTE:因为setBounds方法本身不会触发事件导致重新布局，所以本方法中设置Bounds之后调用了reLayout。
     */
    @Override
    public void reLayout() {
        srpContent.setBounds(CustOpts.HOR_GAP, 
        		CustOpts.VER_GAP, 
        		getWidth() - CustOpts.SIZE_EDGE * 2 - CustOpts.HOR_GAP  * 3, 
                getHeight() - CustOpts.SIZE_TITLE - CustOpts.SIZE_EDGE - CustOpts.VER_GAP * 4  - CustOpts.BTN_HEIGHT);

        btnClose.setBounds(getWidth() - CustOpts.HOR_GAP * 3 - CustOpts.BTN_WIDTH,
                srpContent.getY() + srpContent.getHeight() + CustOpts.VER_GAP, 
                CustOpts.BTN_WIDTH, 
                CustOpts.BTN_HEIGHT);// 关闭
        btnAdd.setBounds(srpContent.getX(), btnClose.getY(), CustOpts.BTN_WIDTH, CustOpts.BTN_HEIGHT);
        btnDelete.setBounds(btnAdd.getX() + btnAdd.getWidth() + CustOpts.HOR_GAP, btnAdd.getY(), CustOpts.BTN_WIDTH,
                CustOpts.BTN_HEIGHT);
        btnModify.setBounds(btnDelete.getX() + btnDelete.getWidth() + CustOpts.HOR_GAP, btnDelete.getY(), CustOpts.BTN_WIDTH,
                CustOpts.BTN_HEIGHT);
        
        IPIMTableColumnModel tTCM = tblContent.getColumnModel();
        tTCM.getColumn(0).setPreferredWidth(40);
        tTCM.getColumn(1).setPreferredWidth(80);
        tTCM.getColumn(2).setPreferredWidth(120);
        tTCM.getColumn(3).setPreferredWidth(40);
        tTCM.getColumn(4).setPreferredWidth(50);
        tTCM.getColumn(5).setPreferredWidth(90);
        tTCM.getColumn(6).setPreferredWidth(60);
        tTCM.getColumn(7).setPreferredWidth(160);
        tTCM.getColumn(8).setPreferredWidth(80);
        tTCM.getColumn(9).setPreferredWidth(120);
        tTCM.getColumn(10).setPreferredWidth(120);
        tTCM.getColumn(11).setPreferredWidth(60);
//        tTCM.getColumn(12).setPreferredWidth(0);
//        tTCM.getColumn(13).setPreferredWidth(0);
//        tTCM.getColumn(14).setPreferredWidth(0);
//        tTCM.getColumn(15).setPreferredWidth(0);
//        tTCM.getColumn(16).setPreferredWidth(0);
//        tTCM.getColumn(17).setPreferredWidth(0);
//        tTCM.getColumn(18).setPreferredWidth(0);
//        tTCM.getColumn(19).setPreferredWidth(0);
//        tTCM.getColumn(20).setPreferredWidth(0);

        validate();
    }

    @Override
    public PIMRecord getContents() {
        return null;
    }

    @Override
    public boolean setContents(
            PIMRecord prmRecord) {
        return true;
    }

    @Override
    public void makeBestUseOfTime() {
    }

    @Override
    public void addAttach(
            File[] file,
            Vector actualAttachFiles) {
    }

    @Override
    public PIMTextPane getTextPane() {
        return null;
    }

    @Override
    public void release() {
        btnClose.removeActionListener(this);
        btnAdd.removeActionListener(this);
        btnDelete.removeActionListener(this);
        btnModify.removeActionListener(this);
        dispose();// 对于对话盒，如果不加这句话，就很难释放掉。
        System.gc();// @TODO:不能允许私自运行gc，应该改为象收邮件线程那样低优先级地自动后台执行，可以从任意方法设置立即执行。
    }

    @Override
    public void componentResized(
            ComponentEvent e) {
        reLayout();
    };

    @Override
    public void componentMoved(
            ComponentEvent e) {
    };

    @Override
    public void componentShown(
            ComponentEvent e) {
    };

    @Override
    public void componentHidden(
            ComponentEvent e) {
    };

    @Override
    public void actionPerformed(
            ActionEvent e) {
        Object o = e.getSource();
        if (o == btnClose) {
            dispose();
        } else if (o == btnAdd) {
            // 创建一个空记录，赋予正确的path值，然后再传给对话盒显示。以确保saveContentAction保存后，记录能显示到正确的地方。
            PIMRecord tRec = new PIMRecord();
            tRec.setFieldValue(PIMPool.pool.getKey(EmployeeDefaultViews.FOLDERID), Integer.valueOf(5002));
            EmployeeDlg tDlg = new EmployeeDlg(this, new SaveContentsAction(), tRec);
            tDlg.setForOneTimeAddition();
            tDlg.setVisible(true);
            initTable();
            reLayout();
        } else if (o == btnDelete) {
            if (JOptionPane.showConfirmDialog(this, BarFrame.consts.COMFIRMDELETEACTION2(), BarFrame.consts.Operator(),
                    JOptionPane.YES_NO_OPTION) != 0)// 确定删除吗？
                return;
            int tSeleRow = tblContent.getSelectedRow();
            String sql = "delete from Employee where ID = " + tblContent.getValueAt(tSeleRow, IDCOLUM);
            try {
                Statement smt = PIMDBModel.getStatement();
                smt.executeUpdate(sql.toString());
                smt.close();
                smt = null;

                initTable();
                reLayout();
            } catch (SQLException exp) {
                exp.printStackTrace();
            }
        } else if(o == btnModify) {
        	modifyEmployee();
        }
    }

    @Override
    public void mouseClicked(
            MouseEvent e) {
        if (e.getClickCount() > 1) {
            modifyEmployee();
        }
    }

	public void modifyEmployee() {
	    new LoginDlg(PosFrame.instance).setVisible(true);// 结果不会被保存到ini
	    if (LoginDlg.PASSED == true) { // 如果用户选择了确定按钮。
	    	BarFrame.instance.valOperator.setText(LoginDlg.USERNAME);
	        if (LoginDlg.USERTYPE >= 2) {// 进一步判断，如果新登陆是经理，弹出对话盒
	            int tRow = tblContent.getSelectedRow();
	            if(tRow < tableModel.length && tRow >= 0) {
		            PIMRecord tRec =
		                    CASControl.ctrl.getModel().selectRecord(CustOpts.custOps.APPNameVec.indexOf("Employee"),
		                            ((Integer) tableModel[tRow][20]).intValue(), 5002); // to select
		                                                                                                // a record
		                                                                                                // from DB.
		            // 不合适重用OpenAction。因为OpenAction的结果是调用View系统的更新机制。而这里需要的是更新list对话盒。
		            new EmployeeDlg(this, new UpdateContactAction(), tRec).setVisible(true);
		            initTable();
		            reLayout();
	            }else {
		        	JOptionPane.showMessageDialog(this, BarFrame.consts.OnlyOneShouldBeSelected());
		        }
	        }
	    }
	}

    @Override
    public void mousePressed(
            MouseEvent e) {
    }

    @Override
    public void mouseReleased(
            MouseEvent e) {
    }

    @Override
    public void mouseEntered(
            MouseEvent e) {
    }

    @Override
    public void mouseExited(
            MouseEvent e) {
    }

    @Override
    public Container getContainer() {
        return getContentPane();
    }

    private void initDialog() {
        setTitle(BarFrame.consts.EmployeeInfo());

        // 初始化－－－－－－－－－－－－－－－－
        tblContent = new PIMTable();// 显示字段的表格,设置模型
        srpContent = new PIMScrollPane(tblContent);
        btnClose = new JButton(BarFrame.consts.Close());
        btnAdd = new JButton(BarFrame.consts.Add());//NewUser());
        btnDelete = new JButton(BarFrame.consts.Delete());
        btnModify = new JButton(BarFrame.consts.Modify());
        
        // properties
        btnClose.setMnemonic('o');
        btnClose.setMargin(new Insets(0, 0, 0, 0));
        btnAdd.setMnemonic('A');
        btnAdd.setMargin(btnClose.getMargin());
        btnDelete.setMnemonic('D');
        btnDelete.setMargin(btnClose.getMargin());
        btnModify.setMnemonic('M');
        btnModify.setMargin(btnClose.getMargin());

        tblContent.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblContent.setAutoscrolls(true);
        tblContent.setRowHeight(20);
        tblContent.setBorder(new JTextField().getBorder());
        tblContent.setFocusable(false);

        JLabel tLbl = new JLabel();
        tLbl.setOpaque(true);
        tLbl.setBackground(Color.GRAY);
        srpContent.setCorner(JScrollPane.LOWER_RIGHT_CORNER, tLbl);
        getRootPane().setDefaultButton(btnClose);

        // 布局---------------
        setBounds((CustOpts.SCRWIDTH - 540) / 2, (CustOpts.SCRHEIGHT - 320) / 2, 540, 320); // 对话框的默认尺寸。
        getContentPane().setLayout(null);

        // 搭建－－－－－－－－－－－－－
        getContentPane().add(srpContent);
        getContentPane().add(btnClose);
        getContentPane().add(btnAdd);
        getContentPane().add(btnDelete);
        getContentPane().add(btnModify);

        // 加监听器－－－－－－－－
        btnClose.addActionListener(this);
        btnAdd.addActionListener(this);
        btnDelete.addActionListener(this);
        btnModify.addActionListener(this);
        btnClose.addKeyListener(this);
        btnAdd.addKeyListener(this);
        btnDelete.addKeyListener(this);
        tblContent.addMouseListener(this);
        getContentPane().addComponentListener(this);
        // initContents--------------
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                initTable();
            }
        });
    }

    private void initTable() {
        String sql =
                "select CODE,NNAME,SUBJECT,SEX,TITLE,CPHONE,PHONE,ADDRESS,CNUMBER,EMAIL,WEBPAGE,CATEGORY,JOINTIME, SALARY, INSURANCE, SSCNUMBER,IDCARD,BIRTHDAY,BANKNUMBER,CONTENT,ID from employee where DELETED != true";

        try {
            ResultSet rs = PIMDBModel.getReadOnlyStatement().executeQuery(sql);
            rs.afterLast();
            rs.relative(-1);
            int tmpPos = rs.getRow();
            tableModel = new Object[tmpPos][21];
            rs.beforeFirst();
            tmpPos = 0;
            while (rs.next()) {
                tableModel[tmpPos][0] = rs.getString("ID");
                tableModel[tmpPos][1] = rs.getString("NNAME");
                tableModel[tmpPos][2] = rs.getString("SUBJECT");
                tableModel[tmpPos][3] = rs.getString("SEX");
                tableModel[tmpPos][4] = rs.getString("TITLE");
                tableModel[tmpPos][5] = rs.getString("CPHONE");
                tableModel[tmpPos][6] = rs.getString("PHONE");
                tableModel[tmpPos][7] = rs.getString("ADDRESS");
                tableModel[tmpPos][8] = rs.getString("CNUMBER");
                tableModel[tmpPos][9] = rs.getString("EMAIL");
                tableModel[tmpPos][10] = rs.getString("WEBPAGE");
                tableModel[tmpPos][11] = rs.getString("CATEGORY");
                tableModel[tmpPos][12] = rs.getDate("JOINTIME");
                tableModel[tmpPos][13] = Integer.valueOf(rs.getInt("SALARY"));
                tableModel[tmpPos][14] = rs.getString("INSURANCE");
                tableModel[tmpPos][15] = rs.getString("SSCNUMBER");
                tableModel[tmpPos][16] = rs.getString("IDCARD");
                tableModel[tmpPos][17] = rs.getDate("BIRTHDAY");
                tableModel[tmpPos][18] = rs.getString("BANKNUMBER");
                tableModel[tmpPos][19] = rs.getString("CONTENT");
                tableModel[tmpPos][20] = rs.getInt("ID");
                tmpPos++;
            }
            rs.close();// 关闭
        } catch (SQLException e) {
            ErrorUtil.write(e);
        }

        tblContent.setDataVector(tableModel, header);
        DefaultPIMTableCellRenderer tCellRender = new DefaultPIMTableCellRenderer();
        tCellRender.setOpaque(true);
        tCellRender.setBackground(Color.LIGHT_GRAY);
        tblContent.getColumnModel().getColumn(1).setCellRenderer(tCellRender);
    }

    private String[] header = new String[] { 
    		ContactDefaultViews.TEXTS[0],
    		BarFrame.consts.NickName(), // "昵称"
            BarFrame.consts.DISPLAYAS(), // "language"
            BarFrame.consts.Sex(), // "性别";
            BarFrame.consts.JobTitle(), // "职位"
            BarFrame.consts.Cellphone(), // "手机"
            BarFrame.consts.PhoneNum(), // "宅电"
            BarFrame.consts.HomeAddress(),// "家庭住址";
            BarFrame.consts.QQ(), // "即时通讯号码"
            BarFrame.consts.MailAddress(),// "电子邮件地址"
            BarFrame.consts.MainPage(),// "主页";
            BarFrame.consts.Type(),// 类别
//            BarFrame.consts.JoinTime,// "进单位时间";
//            BarFrame.consts.Salary,// "工资";
//            BarFrame.consts.INSURANCE,// "保险";
//            BarFrame.consts.SSCNUMBER,// "社保号码";
//            BarFrame.consts.IDCARD,// "身份证";
//            BarFrame.consts.BIRTHDAY,// "生日";
//            BarFrame.consts.BANKNUMBER,// "银行卡号";
//            BarFrame.consts.Note
            }; // "备注"

    PIMTable tblContent;
    PIMScrollPane srpContent;
    private JButton btnAdd;
    private JButton btnDelete;
    private JButton btnModify;
    private JButton btnClose;
}
