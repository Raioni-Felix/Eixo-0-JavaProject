package com.jogo.inventario;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Inventario {

    private final Map<String, Inventariavel> itens; //Utiliza uma tabela hash pra definir os itens dos inventarios -lincoln
    private final Map<String, Integer> quantidades;

    public Inventario() { //Mesma coisa, lembrando que o Hash é encadeado -lincoln
        this.itens = new LinkedHashMap<>();
        this.quantidades = new LinkedHashMap<>();
    }


    //Para adicionar itens no inventario, tem que ver se faz stack ou não
    //Resolvi usando dois tipos de ID na categoria isEmpilhavel
    //Nesse caso ele implementa verificando cada um pelo Id
    //Se for empilhavel, add e aumenta o contador
    //Se não for, adiciona num espaço diferente do inventario -lincoln
    public boolean adicionar(Inventariavel objeto) {
        if (objeto == null) {
            return false;
        }
        String id = objeto.getId();

        if (itens.containsKey(id) && objeto.getCategoria().isEmpilhavel()) {
            quantidades.put(id, quantidades.get(id) + 1);
            return true;
        }

        if (itens.containsKey(id)) {
            return false; // duplicata de algo não-empilhável, só pra garantir que não dê merda -lincoln
        }

        itens.put(id, objeto);
        quantidades.put(id, 1);
        return true;
    }

    //Sobrecarga: adiciona vários de uma vez e diz quantos entraram.
    //"? extends Inventariavel" aceita List<Arma>, List<ModuloCura>...
    //(uma List<Arma> não é uma List<Inventariavel> em Java)
    public int adicionar(List<? extends Inventariavel> objetos) {
        int adicionados = 0;
        for (Inventariavel objeto : objetos) {
            if (adicionar(objeto)) {
                adicionados++;
            }
        }
        return adicionados;
    }

    public boolean remover(String id) { //Remover usando a mesma logica de add só que ao contrario -lincoln
        if (id == null || !itens.containsKey(id)) {
            return false;
        }
        int restante = quantidades.get(id) - 1;
        if (restante <= 0) {
            itens.remove(id);
            quantidades.remove(id);
        } else {
            quantidades.put(id, restante);
        }
        return true;
    }

    public Inventariavel buscar(String id) { //Busca o item no inventario pelo id dele -lincoln
        if (id == null) {
            return null;
        }
        return itens.get(id);
    }

    public int quantidadeDe(String id) {
        return quantidades.getOrDefault(id, 0);
    }

    public boolean temItem(String id) {
        return itens.containsKey(id);
    }

    public int quantidadeItem() {
        return itens.size(); // quantidade de TIPOS distintos -lincoln
    }

    public boolean inventarioVazio() {
        return itens.isEmpty();
    }

    //Lista os itens de um tipo, na ordem em que entraram (o
    //LinkedHashMap guarda a ordem). Método genérico: <T> é o tipo pedido,
    //então listarDoTipo(Arma.class) já devolve List<Arma>, sem cast do
    //lado de quem chama. Usado pelo ComponenteArma pra montar o arsenal.
    public <T> List<T> listarDoTipo(Class<T> tipo) {
        List<T> encontrados = new ArrayList<>();
        for (Inventariavel objeto : itens.values()) {
            if (tipo.isInstance(objeto)) {
                encontrados.add(tipo.cast(objeto));
            }
        }
        return encontrados;
    }

    public Map<String, Inventariavel> getItens() {
        return Map.copyOf(itens);
    }
}