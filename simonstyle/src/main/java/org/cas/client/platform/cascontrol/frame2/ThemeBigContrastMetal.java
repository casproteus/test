package org.cas.client.platform.cascontrol.frame2;

/*
 * @(#)BigContrastMetalTheme.java	1.15 04/07/26
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
 * @(#)BigContrastMetalTheme.java	1.15 04/07/26
 */

import java.awt.Color;
import java.awt.Font;

import javax.swing.UIDefaults;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalIconFactory;

/**
 * This class describes a theme using "green" colors.
 */
public class ThemeBigContrastMetal extends ThemeContrastMetal {

    @Override
    public String getName() {
        return "Low Vision";
    }

    private final FontUIResource controlFont = new FontUIResource("Dialog", Font.PLAIN, 24);
    private final FontUIResource systemFont = new FontUIResource("Dialog", Font.PLAIN, 24);
    private final FontUIResource windowTitleFont = new FontUIResource("Dialog", Font.PLAIN, 24);
    private final FontUIResource userFont = new FontUIResource("SansSerif", Font.PLAIN, 24);
    private final FontUIResource smallFont = new FontUIResource("Dialog", Font.PLAIN, 20);

    @Override
    public FontUIResource getControlTextFont() {
        return controlFont;
    }

    @Override
    public FontUIResource getSystemTextFont() {
        return systemFont;
    }

    @Override
    public FontUIResource getUserTextFont() {
        return userFont;
    }

    @Override
    public FontUIResource getMenuTextFont() {
        return controlFont;
    }

    @Override
    public FontUIResource getWindowTitleFont() {
        return windowTitleFont;
    }

    @Override
    public FontUIResource getSubTextFont() {
        return smallFont;
    }

    @Override
    public void addCustomEntriesToTable(
            UIDefaults table) {
        super.addCustomEntriesToTable(table);

        final int internalFrameIconSize = 30;
        table.put("InternalFrame.closeIcon", MetalIconFactory.getInternalFrameCloseIcon(internalFrameIconSize));
        table.put("InternalFrame.maximizeIcon", MetalIconFactory.getInternalFrameMaximizeIcon(internalFrameIconSize));
        table.put("InternalFrame.iconifyIcon", MetalIconFactory.getInternalFrameMinimizeIcon(internalFrameIconSize));
        table.put("InternalFrame.minimizeIcon", MetalIconFactory.getInternalFrameAltMaximizeIcon(internalFrameIconSize));

        Border blackLineBorder = new BorderUIResource(new MatteBorder(2, 2, 2, 2, Color.black));
        Border textBorder = blackLineBorder;

        table.put("ToolTip.border", blackLineBorder);
        table.put("TitledBorder.border", blackLineBorder);

        table.put("TextField.border", textBorder);
        table.put("PasswordField.border", textBorder);
        table.put("TextArea.border", textBorder);
        table.put("TextPane.font", textBorder);

        table.put("ScrollPane.border", blackLineBorder);

        table.put("ScrollBar.width", new Integer(25));

    }
}
