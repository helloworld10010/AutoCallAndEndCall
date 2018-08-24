package com.example.treesa.autocallandendcall;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Unique;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class Phones {
    @Id(autoincrement = true)
    private Long id;
    @NotNull
    @Unique
    private String number;
    @Generated(hash = 146877604)
    public Phones(Long id, @NotNull String number) {
        this.id = id;
        this.number = number;
    }
    @Generated(hash = 624885063)
    public Phones() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getNumber() {
        return this.number;
    }
    public void setNumber(String number) {
        this.number = number;
    }

    @Override
    public String toString() {
        return number;
    }
}
