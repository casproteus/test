package org.cas.client.platform.bar.dialog;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.cas.client.platform.bar.beans.ArrayButton;
import org.cas.client.platform.bar.beans.CategoryToggleButton;
import org.cas.client.platform.bar.beans.MenuButton;
import org.cas.client.platform.bar.model.Category;
import org.cas.client.platform.bar.model.Dish;
import org.cas.client.platform.bar.model.Printer;
import org.cas.client.platform.bar.print.WifiPrintService;
import org.cas.client.platform.cascontrol.dialog.logindlg.LoginDlg;
import org.cas.client.platform.cascustomize.CustOpts;
import org.cas.client.platform.pimmodel.PIMDBModel;

public class MenuPanel extends JPanel implements ActionListener {
	
    private int curCategoryPage = 0;
    private int categoryNumPerPage = 0;

    private int curMenuPageNum = 0;
    private int curMenuPerPage = 0;

    Integer categoryColumn = (Integer) CustOpts.custOps.hash2.get("categoryColumn");
    Integer categoryRow = (Integer) CustOpts.custOps.hash2.get("categoryRow");
    Integer menuColumn = (Integer) CustOpts.custOps.hash2.get("menuColumn");
    Integer menuRow = (Integer) CustOpts.custOps.hash2.get("menuRow");

    String[][] categoryNameMetrix;
    ArrayList<ArrayList<CategoryToggleButton>> onSrcCategoryTgbMatrix = new ArrayList<ArrayList<CategoryToggleButton>>();
    CategoryToggleButton tgbActiveCategory;
    
    //Dish is more complecated than category, it's devided by category first, then divided by page.
    String[][] dishNameMetrix;// the struction must be [3][index]. it's more convenient than [index][3]
    String[][] onScrDishNameMetrix;// it's sub set of all menuNameMetrix
    private ArrayList<ArrayList<MenuButton>> onSrcMenuBtnMatrix = new ArrayList<ArrayList<MenuButton>>();
    
    private Dish[] dishAry;
    private Dish[] onScrDishAry;
    ArrayList<Dish> selectdDishAry = new ArrayList<Dish>();

	public MenuPanel() {
		
		btnPageUpCategory = new ArrayButton("↑");
        btnPageDownCategory = new ArrayButton("↓");
        btnPageUpMenu = new ArrayButton("↑");
        btnPageDownMenu = new ArrayButton("↓");
        
        btnPageUpCategory.setMargin(new Insets(0,0,0,0));
        btnPageDownCategory.setMargin(btnPageUpCategory.getInsets());
        btnPageUpMenu.setMargin(btnPageUpCategory.getInsets());
        btnPageDownMenu.setMargin(btnPageUpCategory.getInsets());
        
        btnPageUpCategory.setEnabled(false);
        btnPageUpMenu.setEnabled(false);

        btnPageUpCategory.addActionListener(this);
        btnPageDownCategory.addActionListener(this);
        btnPageUpMenu.addActionListener(this);
        btnPageDownMenu.addActionListener(this);
        
        setLayout(null);
        add(btnPageUpCategory);
        add(btnPageDownCategory);
        add(btnPageUpMenu);
        add(btnPageDownMenu);
        
		initCategoryAndDishes();
	}
	
    public void initCategoryAndDishes() {
        try {
            Statement statement = PIMDBModel.getReadOnlyStatement();

            // load all the categorys---------------------------
            ResultSet categoryRS = statement.executeQuery("select ID, LANG1, LANG2, LANG3 from CATEGORY order by DSP_INDEX");
            categoryRS.afterLast();
            categoryRS.relative(-1);
            int tmpPos = categoryRS.getRow();
            categoryNameMetrix = new String[3][tmpPos];
            WifiPrintService.allCategory = new Category[tmpPos];
            categoryRS.beforeFirst();

            tmpPos = 0;
            while (categoryRS.next()) {
                categoryNameMetrix[0][tmpPos] = categoryRS.getString("LANG1");
                categoryNameMetrix[1][tmpPos] = categoryRS.getString("LANG2");
                categoryNameMetrix[2][tmpPos] = categoryRS.getString("LANG3");
                
                WifiPrintService.allCategory[tmpPos] =  new Category();
                WifiPrintService.allCategory[tmpPos].setID(categoryRS.getInt("ID"));
                WifiPrintService.allCategory[tmpPos].setDspIndex(tmpPos);
                WifiPrintService.allCategory[tmpPos].setLanguage(new String[]{categoryNameMetrix[0][tmpPos],
                		categoryNameMetrix[1][tmpPos], categoryNameMetrix[2][tmpPos]});
                
                tmpPos++;
            }
            categoryRS.close();// 关闭

            // load all the dishes----------------------------
            ResultSet productRS =
                    statement
                            .executeQuery("select ID, CODE, MNEMONIC, SUBJECT, PRICE, FOLDERID, STORE,  COST, BRAND, CATEGORY, CONTENT, UNIT, PRODUCAREA, INDEX from product where deleted != true order by index");
            productRS.afterLast();
            productRS.relative(-1);
            tmpPos = productRS.getRow();
            dishNameMetrix = new String[3][tmpPos];
            dishAry = new Dish[tmpPos];
            productRS.beforeFirst();
            
            //compose the record into dish objects--------------
            tmpPos = 0;
            while (productRS.next()) { // @NOTE: don't load all the content, because menu can be many
                dishNameMetrix[0][tmpPos] = productRS.getString("CODE");
                dishNameMetrix[1][tmpPos] = productRS.getString("MNEMONIC");
                dishNameMetrix[2][tmpPos] = productRS.getString("SUBJECT");

                dishAry[tmpPos] = new Dish();
                dishAry[tmpPos].setId(productRS.getInt("ID"));
                dishAry[tmpPos].setLanguage(0, dishNameMetrix[0][tmpPos]);
                dishAry[tmpPos].setLanguage(1, dishNameMetrix[1][tmpPos]);
                dishAry[tmpPos].setLanguage(2, dishNameMetrix[2][tmpPos]);
                dishAry[tmpPos].setPrice(productRS.getInt("PRICE"));
                dishAry[tmpPos].setGst(productRS.getInt("FOLDERID"));
                dishAry[tmpPos].setQst(productRS.getInt("STORE"));
                dishAry[tmpPos].setSize(productRS.getInt("COST"));
                dishAry[tmpPos].setPrinter(productRS.getString("BRAND"));
                dishAry[tmpPos].setCATEGORY(productRS.getString("CATEGORY"));
                dishAry[tmpPos].setPrompPrice(productRS.getString("CONTENT"));
                dishAry[tmpPos].setPrompMenu(productRS.getString("UNIT"));
                dishAry[tmpPos].setPrompMofify(productRS.getString("PRODUCAREA"));
                dishAry[tmpPos].setDspIndex(productRS.getInt("INDEX"));
                tmpPos++;
            }
            productRS.close();// 关闭
            
            //load all printers--------------------------
			String sql = "select ID, Type, UserName, PASSWORD, LANG from useridentity where Type = 'PRINTER'";

			ResultSet rs = statement.executeQuery(sql);

			ResultSetMetaData rd = rs.getMetaData(); // 得到结果集相关信息
			rs.afterLast();
			rs.relative(-1);
			tmpPos = rs.getRow();
			Printer[] printers = new Printer[tmpPos];
			rs.beforeFirst();
			tmpPos = 0;
			while (rs.next()) {
				printers[tmpPos] = new Printer();
				printers[tmpPos].setId(rs.getInt("id"));
				printers[tmpPos].setPname(rs.getString("UserName"));
				printers[tmpPos].setIp(rs.getString("PASSWORD"));
				printers[tmpPos].setFirstPrint(rs.getInt("TYPE")); // index p1, p2.....
				printers[tmpPos].setType(rs.getInt("LANG"));
				tmpPos++;
			}
			rs.close();
			// rearrange into map
			for(Printer printer:printers){
	            WifiPrintService.ipPrinterMap.put(printer.getIp(),printer);
	        }
			
        } catch (Exception e) {
           // ErrorUtil.write(e);
          //TODDO: delete this line
            Printer[] printers = new Printer[1];
			printers[0] = new Printer();
			printers[0].setPname("p1");
			printers[0].setIp("192.168.1.88");
			printers[0].setFirstPrint(1); // index p1, p2.....
			printers[0].setType(0);
			WifiPrintService.ipPrinterMap.put(printers[0].getIp(),printers[0]);
        }
        reInitCategoryAndMenuBtns();
    }

    // menu and category buttons must be init after initContent---------
    private void reInitCategoryAndMenuBtns() {
        // validate rows and columns first(in case they are changed into bad value)--------
        categoryColumn = (categoryColumn == null || categoryColumn < 4) ? 5 : categoryColumn;
        categoryRow = (categoryRow == null || categoryRow < 1 || categoryRow > 9) ? 3 : categoryRow;
        categoryNumPerPage = categoryColumn * categoryRow;

        menuColumn = (menuColumn == null || menuColumn < 1) ? 4 : menuColumn;
        menuRow = (menuRow == null || menuRow < 1) ? 4 : menuRow;
        curMenuPerPage = menuColumn * menuRow;

        // clean current catogory and menus from both screen and metrix if have---------------
        for (int r = 0; r < categoryRow; r++) {
            if (r < onSrcCategoryTgbMatrix.size()) {
                for (int c = 0; c < categoryColumn; c++) {
                    if (c < onSrcCategoryTgbMatrix.get(r).size())
                        remove(onSrcCategoryTgbMatrix.get(r).get(c));
                }
            }
        }
        for (int r = 0; r < menuRow; r++) {
            if (r < onSrcMenuBtnMatrix.size()) {
                for (int c = 0; c < menuColumn; c++) {
                    if (c < onSrcMenuBtnMatrix.get(r).size())
                        remove(onSrcMenuBtnMatrix.get(r).get(c));
                }
            }
        }
        onSrcCategoryTgbMatrix.clear();
        onSrcMenuBtnMatrix.clear();

        // create new buttons and add onto the screen (no layout yet)------------
        int dspIndex = curCategoryPage * categoryNumPerPage;
        for (int r = 0; r < categoryRow; r++) {
            ArrayList<CategoryToggleButton> btnCategoryArry = new ArrayList<CategoryToggleButton>();
            for (int c = 0; c < categoryColumn; c++) {
                dspIndex++;
                CategoryToggleButton btnCategory = new CategoryToggleButton(dspIndex);
                btnCategory.setMargin(new Insets(0, 0, 0, 0));
                add(btnCategory);
                btnCategory.addActionListener(this);
                btnCategoryArry.add(btnCategory);
                if (dspIndex <= categoryNameMetrix[0].length) {
                    btnCategory.setText(categoryNameMetrix[CustOpts.custOps.getUserLang()][dspIndex - 1]);
                    if (tgbActiveCategory != null
                            && categoryNameMetrix[CustOpts.custOps.getUserLang()][dspIndex - 1].equalsIgnoreCase(tgbActiveCategory.getText())) {
                        btnCategory.setSelected(true);
                    }
                } else {
                    btnPageDownCategory.setEnabled(false);
                }
            }
            onSrcCategoryTgbMatrix.add(btnCategoryArry);
        }

        // if no activeCategory, use the first one on screen.
        if (tgbActiveCategory == null) {
            tgbActiveCategory = onSrcCategoryTgbMatrix.get(0).get(0);
            tgbActiveCategory.setSelected(true);
        }

        // initialize on screen menus===============================================================
        //find out menus matching to current category and current lang
        onScrDishNameMetrix = new String[3][dishNameMetrix[0].length];
        onScrDishAry = new Dish[dishNameMetrix[0].length];
        
        int onscrMenuIndex = 0;
        for (int i = 0; i < dishAry.length; i++) {
			if(dishAry[i].getCATEGORY().equals(tgbActiveCategory.getText())) {
				
				onScrDishNameMetrix[0][onscrMenuIndex] = dishNameMetrix[0][i];
				onScrDishNameMetrix[1][onscrMenuIndex] = dishNameMetrix[1][i];
				onScrDishNameMetrix[2][onscrMenuIndex] = dishNameMetrix[2][i];
				
				onScrDishAry[onscrMenuIndex] = dishAry[i];
				//make sure the display index are lined
				if(dishAry[i].getDspIndex() != onscrMenuIndex + 1) {
					try {
		                Statement smt =  PIMDBModel.getReadOnlyStatement();

			            StringBuilder sql = new StringBuilder("UPDATE product SET INDEX = ").append(onscrMenuIndex + 1)
			                            	.append(" where ID = ").append(dishAry[i].getId());
			            smt.executeUpdate(sql.toString());
			            smt.close();
		                smt = null;
		            }catch(Exception exp) {
		                exp.printStackTrace();
		            }
					dishAry[i].setDspIndex(onscrMenuIndex + 1);
				}
				
				onscrMenuIndex++;
			}
		}
        
        dspIndex = curMenuPageNum * curMenuPerPage;
        for (int r = 0; r < menuRow; r++) {
            ArrayList<MenuButton> btnMenuArry = new ArrayList<MenuButton>();
            for (int c = 0; c < menuColumn; c++) {
                MenuButton btnMenu = new MenuButton(dspIndex + 1);
                btnMenu.setMargin(new Insets(0, 0, 0, 0));
                add(btnMenu);
                btnMenu.addActionListener(this);
                btnMenuArry.add(btnMenu);
                if (dspIndex < onscrMenuIndex) {
                    btnMenu.setText(onScrDishNameMetrix[CustOpts.custOps.getUserLang()][dspIndex]);
                    btnMenu.setDish(onScrDishAry[dspIndex]);
                } else {
                    btnPageDownMenu.setEnabled(false);
                }

                dspIndex++;
            }
            onSrcMenuBtnMatrix.add(btnMenuArry);
        }
    }

    public void setBounds(int x, int y, int width, int height) {
    	if(y == 12) {
    		System.out.println("stop!");
    	}
    	super.setBounds(x, y, width, height);
    }
    void reLayout() {
        // category area--------------
        int widthMenuArea = getWidth() - BarDlgConst.SCROLLBAR_WIDTH - CustOpts.HOR_GAP;
        
        Double categoryHeight = (Double) CustOpts.custOps.hash2.get("categoryHeight");
        categoryHeight = (categoryHeight == null || categoryHeight < 0.2) ? 0.4 : categoryHeight;

        int categeryBtnWidth = (widthMenuArea - CustOpts.HOR_GAP * (categoryColumn - 1)) / categoryColumn;
        int categeryBtnHeight =
                (int) ((getHeight() * categoryHeight - CustOpts.VER_GAP * (categoryRow - 1)) / categoryRow);

        for (int r = 0; r < categoryRow; r++) {
            for (int c = 0; c < categoryColumn; c++) {
                JToggleButton toggleButton = onSrcCategoryTgbMatrix.get(r).get(c);
                toggleButton.setBounds((categeryBtnWidth + CustOpts.HOR_GAP) * c,
                        (categeryBtnHeight + CustOpts.VER_GAP) * r, categeryBtnWidth, categeryBtnHeight);
            }
        }
        btnPageUpCategory.setBounds(widthMenuArea, 0, BarDlgConst.SCROLLBAR_WIDTH,
                BarDlgConst.SCROLLBAR_WIDTH * 2);
        btnPageDownCategory.setBounds(btnPageUpCategory.getX(),
                btnPageUpCategory.getY() + btnPageUpCategory.getHeight() + CustOpts.VER_GAP,
                BarDlgConst.SCROLLBAR_WIDTH, BarDlgConst.SCROLLBAR_WIDTH * 2);

        // menu area--------------
        int menuY = (categeryBtnHeight + CustOpts.VER_GAP) * categoryRow + CustOpts.VER_GAP;
        int menuBtnWidth = (widthMenuArea - CustOpts.HOR_GAP * (menuColumn - 1)) / menuColumn;
        int menuBtnHeight = (int) ((getHeight() * (1 - categoryHeight) - CustOpts.VER_GAP * (menuRow + 1)) / menuRow);
        for (int r = 0; r < menuRow; r++) {
            for (int c = 0; c < menuColumn; c++) {
                onSrcMenuBtnMatrix
                        .get(r)
                        .get(c)
                        .setBounds((menuBtnWidth + CustOpts.HOR_GAP) * c,
                                menuY + (menuBtnHeight + CustOpts.VER_GAP) * r, menuBtnWidth, menuBtnHeight);
            }
        }
        btnPageUpMenu.setBounds(btnPageUpCategory.getX(),
        		getHeight() - BarDlgConst.SCROLLBAR_WIDTH * 4 - CustOpts.VER_GAP,
                BarDlgConst.SCROLLBAR_WIDTH,
                BarDlgConst.SCROLLBAR_WIDTH * 2);
        btnPageDownMenu.setBounds(btnPageUpMenu.getX(), btnPageUpMenu.getY() + btnPageUpMenu.getHeight()
                + CustOpts.VER_GAP, BarDlgConst.SCROLLBAR_WIDTH, BarDlgConst.SCROLLBAR_WIDTH * 2);

    }
    
	@Override
	public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();
		if(o instanceof ArrayButton) {
	        if(o == btnPageUpCategory) {
	            curCategoryPage--;
	            // adjust status
	            btnPageDownCategory.setEnabled(true);
	            if (curCategoryPage == 0) {
	                btnPageUpCategory.setEnabled(false);
	            }
	
	            reInitCategoryAndMenuBtns();
	            reLayout();
	        } else if (o == btnPageDownCategory) {
	            curCategoryPage++;
	            // adjust status
	            btnPageUpCategory.setEnabled(true);
	            if (curCategoryPage * categoryNumPerPage > categoryNameMetrix.length) {
	                btnPageDownCategory.setEnabled(false);
	            }
	
	            reInitCategoryAndMenuBtns();
	            reLayout();
	        } else if (o == btnPageUpMenu) {
	            curMenuPageNum--;
	            btnPageDownMenu.setEnabled(true);
	            if (curMenuPageNum == 0) {
	                btnPageUpMenu.setEnabled(false);
	            }
	            reInitCategoryAndMenuBtns();
	            reLayout();
	        } else if (o == btnPageDownMenu) {
	            curMenuPageNum++;
	            btnPageUpMenu.setEnabled(true);
	            if (curMenuPageNum * curMenuPerPage > dishNameMetrix.length) {
	                btnPageDownMenu.setEnabled(false);
	            }
	            reInitCategoryAndMenuBtns();
	            reLayout();
	        }
        }
        // category buttons---------------------------------------------------------------------------------
        else if (o instanceof CategoryToggleButton) {
            CategoryToggleButton categoryToggle = (CategoryToggleButton) o;
            String text = categoryToggle.getText();
            if (text == null || text.length() == 0) { // check if it's empty
                if (LoginDlg.USERTYPE == LoginDlg.ADMIN_STATUS) { // and it's admin mode, add a Category.
                    CategoryDlg addCategoryDlg = new CategoryDlg(BarFrame.instance);
                    addCategoryDlg.setIndex(categoryToggle.getIndex());
                    addCategoryDlg.setVisible(true);
                } else {
                    BarFrame.instance.switchMode(2);
                }
            } else { // if it's not empty
                if (!text.equals(tgbActiveCategory.getText())) {
                    //change active toggle button, and update active menus.
                    if (tgbActiveCategory != null) {
                       tgbActiveCategory.setSelected(false);
                   }
                   tgbActiveCategory = categoryToggle;
                   initCategoryAndDishes();	//fill menu buttons with menus belong to this category.
                   reLayout();
               } else if (LoginDlg.USERTYPE == LoginDlg.ADMIN_STATUS) {
                   CategoryDlg categoryDlg = new CategoryDlg(BarFrame.instance);
                   categoryDlg.setIndex(categoryToggle.getIndex());
                   categoryDlg.setVisible(true);
                }
            }
        }
        // menu buttons------------------------------------------------------------------------------------------
        else if (o instanceof MenuButton) {
            MenuButton menuButton = (MenuButton) o;
            String text = menuButton.getText();
            if (text == null || text.length() == 0) { // check if it's empty
                if (LoginDlg.USERTYPE == LoginDlg.ADMIN_STATUS) { // and it's admin mode, add a Category.
                    new DishDlg(BarFrame.instance, menuButton.getDspIndex()).setVisible(true);
                } else {
                    BarFrame.instance.switchMode(2);
                }
            } else { // if it's not empty
                if (LoginDlg.USERTYPE == LoginDlg.ADMIN_STATUS) {
                    new DishDlg(BarFrame.instance, menuButton.getDish()).setVisible(true);
                } else {
                    // add into table.
                	((SalesPanel)BarFrame.instance.panels[1]).addContentToList(menuButton.getDish());
                }
            }
        } 
	}

    private ArrayButton btnPageUpCategory;
    private ArrayButton btnPageDownCategory;
    private ArrayButton btnPageUpMenu;
    private ArrayButton btnPageDownMenu;
}