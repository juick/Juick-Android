/*
 * Copyright (C) 2008-2022, Juick
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.juick.api.model;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * Created by gerc on 11.03.2016.
 */
public class Pms {

    private List<Chat> pms;

    public Pms() {
        this.pms = Collections.emptyList();
    }

    @NonNull
    public List<Chat> getPms() {
        return pms;
    }
}
