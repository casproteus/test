package org.cas.client.platform.bar.dialog;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.comm.CommPortIdentifier;
import javax.comm.ParallelPort;
import javax.comm.PortInUseException;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.cas.client.platform.bar.beans.ArrayButton;
import org.cas.client.platform.bar.beans.TableToggleButton;
import org.cas.client.platform.bar.beans.MenuButton;
import org.cas.client.platform.bar.beans.TableToggleButton;
import org.cas.client.platform.bar.model.Dish;
import org.cas.client.platform.bar.model.Mark;
import org.cas.client.platform.bar.model.Printer;
import org.cas.client.platform.bar.model.User;
import org.cas.client.platform.bar.model.Category;
import org.cas.client.platform.bar.print.Command;
import org.cas.client.platform.bar.print.WifiPrintService;
import org.cas.client.platform.cascontrol.dialog.logindlg.LoginDlg;
import org.cas.client.platform.cascustomize.CustOpts;
import org.cas.client.platform.casutil.ErrorUtil;
import org.cas.client.platform.casutil.PIMPool;
import org.cas.client.platform.contact.dialog.selectcontacts.SelectedNewMemberDlg;
import org.cas.client.platform.pimmodel.PIMDBModel;
import org.cas.client.platform.pimview.pimscrollpane.PIMScrollPane;
import org.cas.client.platform.pimview.pimtable.DefaultPIMTableCellRenderer;
import org.cas.client.platform.pimview.pimtable.PIMTable;
import org.cas.client.platform.pimview.pimtable.PIMTableColumn;
import org.cas.client.platform.pimview.pimtable.PIMTableRenderAgent;
import org.cas.client.platform.pos.dialog.statistics.Statistic;
import org.cas.client.platform.refund.dialog.RefundDlg;
import org.cas.client.resource.international.DlgConst;
import org.cas.client.resource.international.PaneConsts;

//Identity表应该和Employ表合并。
public class TablesPanel extends JPanel implements ComponentListener, ActionListener, FocusListener {

    ArrayList<TableToggleButton> btnTables = new ArrayList<TableToggleButton>();
    
    private final int USER_STATUS = 1;
    private final int ADMIN_STATUS = 2;
    private int curSecurityStatus = USER_STATUS;
    
    User curUser;
    
    Integer tableColumn = (Integer) CustOpts.custOps.hash2.get("tableColumn");
    Integer tableRow = (Integer) CustOpts.custOps.hash2.get("tableRow");

    public static String startTime;

    //flags
    NumberPanelDlg numberPanelDlg; 
    
    //for print
    public static String SUCCESS = "0";
    public static String ERROR = "2";
    
    
    public TablesPanel() {
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
    public void componentMoved(
            ComponentEvent e) {
    }

    /** Invoked when the component has been made visible. */
    @Override
    public void componentShown(
            ComponentEvent e) {
    }

    /** Invoked when the component has been made invisible. */
    @Override
    public void componentHidden(
            ComponentEvent e) {
    }

    @Override
    public void focusGained(
            FocusEvent e) {
        Object o = e.getSource();
        if (o instanceof JTextField)
            ((JTextField) o).selectAll();
    }

    @Override
    public void focusLost(
            FocusEvent e) {
    }

    // ActionListner-------------------------------
    @Override
    public void actionPerformed(
            ActionEvent e) {
        Object o = e.getSource();
        // category buttons---------------------------------------------------------------------------------
        if (o instanceof TableToggleButton) {
            TableToggleButton tableToggle = (TableToggleButton) o;
            
            String text = tableToggle.getText();
            int num = tableToggle.getBillCount();
            if (num == 0) { // check if it's empty
                if (curSecurityStatus == ADMIN_STATUS) { // and it's admin mode, add a Category.
//                    CategoryDlg addCategoryDlg = new CategoryDlg(BarFrame.instance);
//                    addCategoryDlg.setIndex(tableToggle.getIndex());
//                    addCategoryDlg.setVisible(true);
                } else {
                    BarFrame.curTable = text;
                    BarFrame.instance.switchMode(1);
                }
            } else { // if it's not empty
                
            }
        }
        //JButton------------------------------------------------------------------------------------------------
        else if (o instanceof JButton) {
        	if (o == btnLine_1_1) {
            } else if (o == btnLine_1_7) { 
            } else if (o == btnLine_2_4) {
            } else if (o == btnLine_2_5) {
            } else if (o == btnLine_2_6) {
            } else if (o == btnLine_2_7) {
            }else if (o == btnLine_2_8) {
            }else if (o == btnLine_2_9) {
            }
        }
        //JToggleButton-------------------------------------------------------------------------------------
        else if(o instanceof JToggleButton) {
        	if(o == btnLine_1_4) {
        	}else if (o == btnLine_1_5) {
        		
        	}else if (o == btnLine_1_7) {
        	}
        }
    }

    void reLayout() {
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        lblOperator.setBounds(CustOpts.HOR_GAP, CustOpts.VER_GAP, lblOperator.getPreferredSize().width,
                lblOperator.getPreferredSize().height);
        int tBtnWidht = (panelWidth - CustOpts.HOR_GAP * 10) / 9;
        int tBtnHeight = panelHeight / 10;

        lblStartTime.setBounds(panelWidth - lblStartTime.getPreferredSize().width - CustOpts.HOR_GAP,
                lblOperator.getY(), lblStartTime.getPreferredSize().width, lblOperator.getHeight());

        // command buttons--------------
        // line 2
        btnLine_2_1.setBounds(CustOpts.HOR_GAP, panelHeight - tBtnHeight - CustOpts.VER_GAP, tBtnWidht,
                tBtnHeight);
        btnLine_2_2.setBounds(btnLine_2_1.getX() + tBtnWidht + CustOpts.HOR_GAP, btnLine_2_1.getY(), tBtnWidht,
                tBtnHeight);
        btnLine_2_3.setBounds(btnLine_2_2.getX() + tBtnWidht + CustOpts.HOR_GAP, btnLine_2_1.getY(), tBtnWidht,
                tBtnHeight);
        btnLine_2_4.setBounds(btnLine_2_3.getX() + tBtnWidht + CustOpts.HOR_GAP, btnLine_2_1.getY(), tBtnWidht,
                tBtnHeight);
        btnLine_2_5.setBounds(btnLine_2_4.getX() + tBtnWidht + CustOpts.HOR_GAP, btnLine_2_1.getY(), tBtnWidht,
                tBtnHeight);
        btnLine_2_6.setBounds(btnLine_2_5.getX() + tBtnWidht + CustOpts.HOR_GAP, btnLine_2_1.getY(), tBtnWidht,
                tBtnHeight);
        btnLine_2_7.setBounds(btnLine_2_6.getX() + tBtnWidht + CustOpts.HOR_GAP, btnLine_2_1.getY(), tBtnWidht,
                tBtnHeight);
        btnLine_2_8.setBounds(btnLine_2_7.getX() + tBtnWidht + CustOpts.HOR_GAP, btnLine_2_1.getY(), tBtnWidht,
                tBtnHeight);
        btnLine_2_9.setBounds(btnLine_2_8.getX() + tBtnWidht + CustOpts.HOR_GAP, btnLine_2_1.getY(), tBtnWidht,
                tBtnHeight);
        // line 1
        btnLine_1_1.setBounds(CustOpts.HOR_GAP, btnLine_2_1.getY() - tBtnHeight - CustOpts.VER_GAP, tBtnWidht,
                tBtnHeight);
        btnLine_1_2.setBounds(btnLine_1_1.getX() + tBtnWidht + CustOpts.HOR_GAP, btnLine_1_1.getY(), tBtnWidht,
                tBtnHeight);
        btnLine_1_3.setBounds(btnLine_1_2.getX() + tBtnWidht + CustOpts.HOR_GAP, btnLine_1_1.getY(), tBtnWidht,
                tBtnHeight);
        btnLine_1_4.setBounds(btnLine_1_3.getX() + tBtnWidht + CustOpts.HOR_GAP, btnLine_1_1.getY(), tBtnWidht,
                tBtnHeight);
        btnLine_1_5.setBounds(btnLine_1_4.getX() + tBtnWidht + CustOpts.HOR_GAP, btnLine_1_1.getY(), tBtnWidht,
                tBtnHeight);
        btnLine_1_6.setBounds(btnLine_1_5.getX() + tBtnWidht + CustOpts.HOR_GAP, btnLine_1_1.getY(), tBtnWidht,
                tBtnHeight);
        btnLine_1_7.setBounds(btnLine_1_6.getX() + tBtnWidht + CustOpts.HOR_GAP, btnLine_1_1.getY(), tBtnWidht,
                tBtnHeight);
        btnLine_1_8.setBounds(btnLine_1_7.getX() + tBtnWidht + CustOpts.HOR_GAP, btnLine_1_1.getY(), tBtnWidht,
                tBtnHeight);
        btnLine_1_9.setBounds(btnLine_1_8.getX() + tBtnWidht + CustOpts.HOR_GAP, btnLine_1_1.getY(), tBtnWidht,
                tBtnHeight);
    }

    private boolean adminAuthentication() {
        new LoginDlg(null).setVisible(true);
        if (LoginDlg.PASSED == true) { // 如果用户选择了确定按钮。
            if ("System".equalsIgnoreCase(LoginDlg.USERNAME)) {
                curSecurityStatus++;
                BarFrame.setStatusMes(BarDlgConst.ADMIN_MODE);
                // @TODO: might need to do some modification on the interface.
                revalidate();
                return true;
            }
        }
        return false;
    }

    private void initComponent() {
        startTime = Calendar.getInstance().getTime().toLocaleString();
        lblOperator = new JLabel();
        lblStartTime = new JLabel(BarDlgConst.StartTime.concat(BarDlgConst.Colon).concat(startTime));// @Todo:以后改为从服务器上获取。
        
        btnLine_1_1 = new JButton(BarDlgConst.EXACT_AMOUNT);
        btnLine_1_2 = new JButton(BarDlgConst.CASH);
        btnLine_1_3 = new JButton(BarDlgConst.PAY);
        btnLine_1_4 = new JToggleButton("");//BarDlgConst.REMOVE);
        btnLine_1_5 = new JToggleButton("");//BarDlgConst.VOID_ITEM);
        btnLine_1_6 = new JButton(BarDlgConst.SPLIT_BILL);
        btnLine_1_7 = new JToggleButton(BarDlgConst.QTY);
        btnLine_1_8 = new JButton(BarDlgConst.DISC_ITEM);
        btnLine_1_9 = new JButton(BarDlgConst.PRINT_BILL);
        
        btnLine_2_1 = new JButton(BarDlgConst.DEBIT);
        btnLine_2_2 = new JButton(BarDlgConst.VISA);
        btnLine_2_3 = new JButton(BarDlgConst.MASTER);
        btnLine_2_4 = new JButton(BarDlgConst.CANCEL_ALL);
        btnLine_2_5 = new JButton(BarDlgConst.VOID_ORDER);
        btnLine_2_6 = new JButton(BarDlgConst.SETTINGS);
        btnLine_2_7 = new JButton(BarDlgConst.RETURN);
        btnLine_2_8 = new JButton(BarDlgConst.MORE);
        btnLine_2_9 = new JButton(BarDlgConst.SEND);
        
        

        // border----------
        setLayout(null);

        // built
        add(lblOperator);
        add(lblStartTime);

        add(btnLine_2_1);
        add(btnLine_2_2);
        add(btnLine_2_3);
        add(btnLine_2_4);
        add(btnLine_2_5);
        add(btnLine_2_6);
        add(btnLine_2_7);
        add(btnLine_2_8);
        add(btnLine_2_9);

        add(btnLine_1_1);
        add(btnLine_1_2);
        add(btnLine_1_3);
        add(btnLine_1_4);
        add(btnLine_1_5);
        add(btnLine_1_6);
        add(btnLine_1_7);
        add(btnLine_1_8);
        add(btnLine_1_9);

        // add listener
        addComponentListener(this);

        btnLine_2_1.addActionListener(this);
        btnLine_2_2.addActionListener(this);
        btnLine_2_3.addActionListener(this);
        btnLine_2_4.addActionListener(this);
        btnLine_2_5.addActionListener(this);
        btnLine_2_6.addActionListener(this);
        btnLine_2_7.addActionListener(this);
        btnLine_2_8.addActionListener(this);
        btnLine_2_9.addActionListener(this);

        btnLine_1_1.addActionListener(this);
        btnLine_1_2.addActionListener(this);
        btnLine_1_3.addActionListener(this);
        btnLine_1_4.addActionListener(this);
        btnLine_1_5.addActionListener(this);
        btnLine_1_6.addActionListener(this);
        btnLine_1_7.addActionListener(this);
        btnLine_1_8.addActionListener(this);
        btnLine_1_9.addActionListener(this);
        
        // initContents--------------
        reInitTableBtns();
    }

    // menu and category buttons must be init after initContent---------
	private void reInitTableBtns() {
		try {
            Connection connection = PIMDBModel.getConection();
            Statement statement =
                    connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            // load all the categorys---------------------------
            ResultSet categoryRS = statement.executeQuery("select ID, Name, posX, posY, width, height, type from dining_Table order by DSP_INDEX");
            categoryRS.beforeFirst();

            int tmpPos = 0;
            while (categoryRS.next()) {
            	TableToggleButton tableToggleButton = new TableToggleButton();
            	tableToggleButton.setIndex(tmpPos);
            	tableToggleButton.setText(categoryRS.getString("Name"));
            	tableToggleButton.setBounds(categoryRS.getInt("posX"), categoryRS.getInt("posY"), 
            			categoryRS.getInt("width"), categoryRS.getInt("height"));
            	tableToggleButton.setType(categoryRS.getInt("type"));

            	tableToggleButton.setMargin(new Insets(0, 0, 0, 0));
    			tableToggleButton.addActionListener(this);
    			add(tableToggleButton);
    			
            	btnTables.add(tableToggleButton);
                tmpPos++;
            }
            
            categoryRS.close();// 关闭
            statement.close();
		}catch(Exception e) {
			ErrorUtil.write("Unexpected exception when init the tables from db." + e);
		}
	}

    private JLabel lblOperator;
    private JLabel lblStartTime;

    private JButton btnLine_1_1;
    private JButton btnLine_1_2;
    private JButton btnLine_1_3;
    private JToggleButton btnLine_1_4;
    private JToggleButton btnLine_1_5;
    private JButton btnLine_1_6;
    private JToggleButton btnLine_1_7;
    private JButton btnLine_1_8;
    private JButton btnLine_1_9;
    
    private JButton btnLine_2_1;
    private JButton btnLine_2_2;
    private JButton btnLine_2_3;
    private JButton btnLine_2_4;
    private JButton btnLine_2_5;
    private JButton btnLine_2_6;
    private JButton btnLine_2_7;
    private JButton btnLine_2_8;
    private JButton btnLine_2_9;
}