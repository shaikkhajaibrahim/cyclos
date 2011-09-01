/*
    This file is part of Cyclos <http://www.cyclos.org>, made available
    by the Stro organization <http://www.socialtrade.org>.

    Cyclos is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Cyclos is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with Cyclos. If not, see <http://www.gnu.org/licenses/>.

 */
package nl.strohalm.cyclos.utils;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;

import nl.strohalm.cyclos.CyclosConfiguration;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.ServletContextAware;

import com.google.code.kaptcha.GimpyEngine;
import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.text.WordRenderer;
import com.google.code.kaptcha.util.Config;

/**
 * Custom captcha producer which is based in a background image
 * @author luis
 */
public class CaptchaProducer implements Producer, ServletContextAware, InitializingBean {

    private BufferedImage  background;
    private WordRenderer   wordRenderer;
    private GimpyEngine    gimpyEngine;
    private Config         config;
    private ServletContext servletContext;

    public void afterPropertiesSet() throws Exception {
        config = new Config(CyclosConfiguration.getCyclosProperties());

        wordRenderer = config.getWordRendererImpl();
        gimpyEngine = config.getObscurificatorImpl();
    }

    public void clearCache() {
        background = null;
    }

    public BufferedImage createImage(final String text) {
        final BufferedImage background = readBackground();
        BufferedImage image = wordRenderer.renderWord(text, background.getWidth(), background.getHeight());
        image = gimpyEngine.getDistortedImage(image);
        return combine(image, background);
    }

    public String createText() {
        return config.getTextProducerImpl().getText();
    }

    public void setServletContext(final ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * Draws the image over the background
     */
    private BufferedImage combine(final BufferedImage image, final BufferedImage background) {
        final int width = background.getWidth();
        final int height = background.getHeight();

        // Create the new combined image
        final BufferedImage imageWithBackground = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graph = (Graphics2D) imageWithBackground.getGraphics();
        final RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hints.add(new RenderingHints(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY));
        hints.add(new RenderingHints(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY));
        hints.add(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        graph.setRenderingHints(hints);

        graph.fill(new Rectangle2D.Double(0, 0, width, height));

        // draw the image over the background
        graph.drawImage(background, 0, 0, null);
        graph.drawImage(image, 0, 0, null);

        return imageWithBackground;
    }

    private BufferedImage readBackground() {
        if (background == null) {
            try {
                final URL backgroundUrl = servletContext.getResource("/pages/images/captchaBackground.jpg");
                background = ImageIO.read(backgroundUrl);
            } catch (final Exception e) {
                throw new IllegalStateException("Could not read captcha background image");
            }
        }
        return background;
    }

}
