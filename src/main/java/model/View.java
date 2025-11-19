package model;

public class View {
    private String name;
    private String template;
    private Object data;    

    public void setData(Object data) {
        this.data = data;
    }
    public Object getData() {
        return data;
    }

    public void setTemplate(String template){
        this.template = template;
    }
    public String getTemplate() {
        return template;
    }

    public void setName(String name){
        this.name = name;
    }
    public String getName() {
        return name;
    }

    public View(String template) {
        setTemplate(template);
    }

    public View(String name, String template) {
        setName(name);
        setTemplate(template);
    }
    
    public View(String name, String template, Object data) {
        setName(name);
        setTemplate(template);
        setData(data);
    }

    public View() {
    }

}
