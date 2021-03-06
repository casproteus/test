package org.cas.client.platform.cascontrol.frame2;

/*
 * @(#)ContrastMetalTheme.java	1.18 04/07/26
 * 
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * -Redistribution of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 * 
 * -Redistribution in binary form must reproduce the above copyright notice, 
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may 
 * be used to endorse or promote products derived from this software without 
 * specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL 
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MIDROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST 
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, 
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY 
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, 
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed, licensed or intended
 * for use in the design, construction, operation or maintenance of any
 * nuclear facility.
 */

/*
 * @(#)ContrastMetalTheme.java	1.18 04/07/26
 */

import javax.swing.UIDefaults;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.plaf.metal.DefaultMetalTheme;

/**
 * This class describes a higher-contrast Metal Theme.
 */

public class ThemeContrastMetal extends DefaultMetalTheme {

    @Override
    public String getName() {
        return "Contrast";
    }

    private final ColorUIResource primary1 = new ColorUIResource(0, 0, 0);
    private final ColorUIResource primary2 = new ColorUIResource(204, 204, 204);
    private final ColorUIResource primary3 = new ColorUIResource(255, 255, 255);
    private final ColorUIResource primaryHighlight = new ColorUIResource(102, 102, 102);

    private final ColorUIResource secondary2 = new ColorUIResource(204, 204, 204);
    private final ColorUIResource secondary3 = new ColorUIResource(255, 255, 255);
    private final ColorUIResource controlHighlight = new ColorUIResource(102, 102, 102);

    @Override
    protected ColorUIResource getPrimary1() {
        return primary1;
    }

    @Override
    protected ColorUIResource getPrimary2() {
        return primary2;
    }

    @Override
    protected ColorUIResource getPrimary3() {
        return primary3;
    }

    @Override
    public ColorUIResource getPrimaryControlHighlight() {
        return primaryHighlight;
    }

    @Override
    protected ColorUIResource getSecondary2() {
        return secondary2;
    }

    @Override
    protected ColorUIResource getSecondary3() {
        return secondary3;
    }

    @Override
    public ColorUIResource getControlHighlight() {
        return super.getSecondary3();
    }

    @Override
    public ColorUIResource getFocusColor() {
        return getBlack();
    }

    @Override
    public ColorUIResource getTextHighlightColor() {
        return getBlack();
    }

    @Override
    public ColorUIResource getHighlightedTextColor() {
        return getWhite();
    }

    @Override
    public ColorUIResource getMenuSelectedBackground() {
        return getBlack();
    }

    @Override
    public ColorUIResource getMenuSelectedForeground() {
        return getWhite();
    }

    @Override
    public ColorUIResource getAcceleratorForeground() {
        return getBlack();
    }

    @Override
    public ColorUIResource getAcceleratorSelectedForeground() {
        return getWhite();
    }

    @Override
    public void addCustomEntriesToTable(
            UIDefaults table) {

        Border blackLineBorder = new BorderUIResource(new LineBorder(getBlack()));
        Border whiteLineBorder = new BorderUIResource(new LineBorder(getWhite()));

        Object textBorder = new BorderUIResource(new CompoundBorder(blackLineBorder, new BasicBorders.MarginBorder()));

        table.put("ToolTip.border", blackLineBorder);
        table.put("TitledBorder.border", blackLineBorder);
        table.put("Table.focusCellHighlightBorder", whiteLineBorder);
        table.put("Table.focusCellForeground", getWhite());

        table.put("TextField.border", textBorder);
        table.put("PasswordField.border", textBorder);
        table.put("TextArea.border", textBorder);
        table.put("TextPane.font", textBorder);

    }

}
