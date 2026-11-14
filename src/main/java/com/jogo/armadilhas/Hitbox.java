package com.jogo.armadilhas;

import java.awt.Rectangle;

/**
 * Caixa delimitadora utilizada para cálculo de colisões de dano e áreas de gatilho (trigger box).
 *
 * Suporta deslocamentos (offsetX e offsetY) em relação à posição da entidade proprietária,
 * permitindo que a arte visual (sprite) seja maior que a hitbox física real.
 * Isso elimina a frustração de mortes injustas ("pixel clipping") para o jogador.
 */
public class Hitbox {

    private double offsetX;
    private double offsetY;
    private double width;
    private double height;

    public Hitbox(double offsetX, double offsetY, double width, double height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Dimensões da hitbox não podem ser negativas: " + width + "x" + height);
        }
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.width = width;
        this.height = height;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(double offsetX) {
        this.offsetX = offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        if (width < 0) throw new IllegalArgumentException("Largura não pode ser negativa: " + width);
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        if (height < 0) throw new IllegalArgumentException("Altura não pode ser negativa: " + height);
        this.height = height;
    }

    public double getMinX(double entityX) {
        return entityX + offsetX;
    }

    public double getMaxX(double entityX) {
        return entityX + offsetX + width;
    }

    public double getMinY(double entityY) {
        return entityY + offsetY;
    }

    public double getMaxY(double entityY) {
        return entityY + offsetY + height;
    }

    /**
     * Verifica sobreposição (AABB - Axis-Aligned Bounding Box) com outra hitbox.
     */
    public boolean intersects(double thisEntityX, double thisEntityY,
                              Hitbox other, double otherEntityX, double otherEntityY) {
        if (other == null) {
            return false;
        }
        return getMinX(thisEntityX) < other.getMaxX(otherEntityX)
                && getMaxX(thisEntityX) > other.getMinX(otherEntityX)
                && getMinY(thisEntityY) < other.getMaxY(otherEntityY)
                && getMaxY(thisEntityY) > other.getMinY(otherEntityY);
    }

    /**
     * Converte para um java.awt.Rectangle para integração com renderizadores AWT / Swing.
     */
    public Rectangle toAwtRectangle(double entityX, double entityY) {
        return new Rectangle(
                (int) Math.round(getMinX(entityX)),
                (int) Math.round(getMinY(entityY)),
                (int) Math.round(width),
                (int) Math.round(height)
        );
    }

    @Override
    public String toString() {
        return String.format("Hitbox[offset=(%.1f, %.1f), size=%.1fx%.1f]", offsetX, offsetY, width, height);
    }
}
