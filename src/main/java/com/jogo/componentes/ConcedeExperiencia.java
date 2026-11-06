package com.jogo.componentes;

//Trazida do barebones: quem dá XP ao ser derrotado. Implementada por
//ComponenteInimigo; cada tipo de inimigo diz quanto vale.
public interface ConcedeExperiencia {
    int getExperienciaConcedida();
}
