package com.screenshottool.core;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * SelectorDeAreaBridge - Selector de área Swing usado solo con backend Robot.
 *
 * Usa JFrame con setUndecorated(true) para recibir foco de teclado en Windows.
 *
 * FIX: Eliminado KeyboardFocusManager — interceptaba TODAS las teclas del
 * sistema bloqueando la escritura en otras ventanas.
 * FIX: mouseMoved solo repinta cuando el cursor está cerca del botón X,
 * evitando parpadeo constante de pantalla completa.
 */
class SelectorDeAreaBridge extends JFrame {

    private Point inicio;
    private Point fin;
    private Rectangle seleccion;
    private boolean hoverBotonX = false; // estado hover del botón X
    private BufferedImage fondoPantalla;
    private JPanel panel;
    private Rectangle boundsTotal;
    private final AppContext app;

    SelectorDeAreaBridge(AppContext app) throws Exception {
        this.app = app;

        setUndecorated(true);

        // ── Detectar todas las pantallas ──────────────────────
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] pantallas = ge.getScreenDevices();

        boundsTotal = new Rectangle();
        for (GraphicsDevice gd : pantallas) {
            boundsTotal = boundsTotal.union(gd.getDefaultConfiguration().getBounds());
        }

        // ── Captura del escritorio completo ───────────────────
        Robot robot = new Robot();
        Thread.sleep(80);
        fondoPantalla = robot.createScreenCapture(boundsTotal);

        // ── Configurar ventana ────────────────────────────────
        setAlwaysOnTop(true);
        setBounds(boundsTotal);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        final int numP = pantallas.length;
        final GraphicsDevice[] pArray = pantallas;

        // ── Panel de dibujo ───────────────────────────────────
        panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Fondo oscurecido
                g2.drawImage(fondoPantalla, 0, 0, null);
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Bordes entre pantallas
                if (numP > 1) {
                    g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER, 10, new float[] { 6, 4 }, 0));
                    for (int i = 0; i < pArray.length; i++) {
                        Rectangle b = pArray[i].getDefaultConfiguration().getBounds();
                        int rx = b.x - boundsTotal.x;
                        int ry = b.y - boundsTotal.y;
                        g2.setColor(new Color(255, 255, 255, 40));
                        g2.drawRect(rx, ry, b.width - 1, b.height - 1);
                        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                        String etiqueta = "Pantalla " + (i + 1) + "  (" + b.width + "x" + b.height + ")";
                        g2.setColor(new Color(0, 0, 0, 100));
                        g2.fillRoundRect(rx + 8, ry + 8, 180, 22, 6, 6);
                        g2.setColor(new Color(255, 255, 255, 150));
                        g2.drawString(etiqueta, rx + 14, ry + 23);
                    }
                }

                // Área seleccionada
                if (seleccion != null) {
                    g2.drawImage(fondoPantalla,
                            seleccion.x, seleccion.y,
                            seleccion.x + seleccion.width, seleccion.y + seleccion.height,
                            seleccion.x, seleccion.y,
                            seleccion.x + seleccion.width, seleccion.y + seleccion.height, null);

                    g2.setColor(new Color(30, 144, 255));
                    g2.setStroke(new BasicStroke(2));
                    g2.drawRect(seleccion.x, seleccion.y, seleccion.width, seleccion.height);

                    int cs = 10;
                    g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(seleccion.x, seleccion.y, seleccion.x + cs, seleccion.y);
                    g2.drawLine(seleccion.x, seleccion.y, seleccion.x, seleccion.y + cs);
                    g2.drawLine(seleccion.x + seleccion.width - cs, seleccion.y, seleccion.x + seleccion.width,
                            seleccion.y);
                    g2.drawLine(seleccion.x + seleccion.width, seleccion.y, seleccion.x + seleccion.width,
                            seleccion.y + cs);
                    g2.drawLine(seleccion.x, seleccion.y + seleccion.height - cs, seleccion.x,
                            seleccion.y + seleccion.height);
                    g2.drawLine(seleccion.x, seleccion.y + seleccion.height, seleccion.x + cs,
                            seleccion.y + seleccion.height);
                    g2.drawLine(seleccion.x + seleccion.width, seleccion.y + seleccion.height - cs,
                            seleccion.x + seleccion.width, seleccion.y + seleccion.height);
                    g2.drawLine(seleccion.x + seleccion.width - cs, seleccion.y + seleccion.height,
                            seleccion.x + seleccion.width, seleccion.y + seleccion.height);

                    // Tooltip dimensiones
                    String dims = seleccion.width + " x " + seleccion.height + " px";
                    g2.setFont(new Font("Monospaced", Font.BOLD, 12));
                    FontMetrics fm = g2.getFontMetrics();
                    int tw = fm.stringWidth(dims) + 12;
                    int tx = Math.max(0, seleccion.x + seleccion.width - tw);
                    int ty = seleccion.y > 26 ? seleccion.y - 26 : seleccion.y + 6;
                    g2.setColor(new Color(30, 144, 255, 220));
                    g2.fillRoundRect(tx, ty, tw, 20, 6, 6);
                    g2.setColor(Color.WHITE);
                    g2.drawString(dims, tx + 6, ty + 14);
                }

                // Botón X para cancelar (esquina superior derecha)
                int btnX = getWidth() - 46;
                int btnY = 10;
                g2.setColor(hoverBotonX
                        ? new Color(220, 50, 50, 230)
                        : new Color(180, 40, 40, 180));
                g2.fillRoundRect(btnX, btnY, 36, 30, 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                g2.drawString("✕", btnX + 10, btnY + 21);

                // Barra inferior de instrucciones
                String msg = numP > 1
                        ? numP + " pantallas  |  Arrastra para seleccionar  |  ESC cancela"
                        : "Arrastra para seleccionar  |  ESC cancela";
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int msgW = fm.stringWidth(msg) + 28;
                int msgX = (getWidth() - msgW) / 2;
                g2.setColor(new Color(0, 0, 0, 170));
                g2.fillRoundRect(msgX, getHeight() - 46, msgW, 30, 10, 10);
                g2.setColor(Color.WHITE);
                g2.drawString(msg, msgX + 14, getHeight() - 26);
            }
        };

        panel.setOpaque(false);
        add(panel);
        panel.setFocusable(true);

        // ── Mouse ─────────────────────────────────────────────
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                panel.requestFocus();
                // Clic en botón X → cancelar
                int btnX = getWidth() - 46;
                if (e.getX() >= btnX && e.getX() <= btnX + 36 &&
                        e.getY() >= 10 && e.getY() <= 40) {
                    cerrar();
                    return;
                }
                inicio = e.getPoint();
                fin = e.getPoint();
                seleccion = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                fin = e.getPoint();
                seleccion = crearRect(inicio, fin);
                panel.repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                // FIX: solo repintar cuando cambia el estado hover del botón X
                // Evita repaint de pantalla completa en cada movimiento del mouse
                int btnX = getWidth() - 46;
                boolean nuevoHover = e.getX() >= btnX && e.getX() <= btnX + 36 &&
                        e.getY() >= 10 && e.getY() <= 40;
                if (nuevoHover != hoverBotonX) {
                    hoverBotonX = nuevoHover;
                    panel.repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                fin = e.getPoint();
                seleccion = crearRect(inicio, fin);
                if (seleccion != null)
                    procesarSeleccion();
            }
        };
        panel.addMouseListener(mouse);
        panel.addMouseMotionListener(mouse);

        // ── ESC en panel y frame ──────────────────────────────
        // Sin KeyboardFocusManager — ese interceptaba TODAS las teclas
        // del sistema bloqueando escritura en otras ventanas
        KeyAdapter escAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                    cerrar();
            }
        };
        addKeyListener(escAdapter);
        panel.addKeyListener(escAdapter);

        // ── Mostrar ───────────────────────────────────────────
        setFocusableWindowState(true);
        setVisible(true);
        SwingUtilities.invokeLater(() -> panel.requestFocusInWindow());
    }

    // ── Cerrar limpiamente ────────────────────────────────────
    private void cerrar() {
        setFocusableWindowState(false);
        dispose();
    }

    private Rectangle crearRect(Point p1, Point p2) {
        int x = Math.min(p1.x, p2.x), y = Math.min(p1.y, p2.y);
        int w = Math.abs(p1.x - p2.x), h = Math.abs(p1.y - p2.y);
        return (w < 2 || h < 2) ? null : new Rectangle(x, y, w, h);
    }

    private void procesarSeleccion() {
        if (seleccion == null)
            return;
        Rectangle area = new Rectangle(
                boundsTotal.x + seleccion.x,
                boundsTotal.y + seleccion.y,
                seleccion.width,
                seleccion.height);
        cerrar();
        app.mostrarDialogoConArea(area);
    }
}
