package com.example.espzera;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "esp_app.db";
    // IMPORTANTE: Mantenha a versão 2 (ou superior) para que o onUpgrade seja chamado
    private static final int DATABASE_VERSION = 2;

    // Nomes das tabelas
    public static final String TABLE_USUARIO = "usuario";
    public static final String TABLE_AMBIENTE = "ambiente";
    public static final String TABLE_PONTO = "ponto";
    public static final String TABLE_LEITURA = "leitura";
    public static final String TABLE_LINHA = "linha";
    public static final String TABLE_CSI_DATA = "csi_data";

    // Colunas da tabela USUARIO
    public static final String COL_USUARIO_ID = "id";
    public static final String COL_USUARIO_NOME = "nome";
    public static final String COL_USUARIO_APELIDO = "apelido";
    public static final String COL_USUARIO_STATUS = "status";

    // Colunas da tabela AMBIENTE
    public static final String COL_AMBIENTE_ID = "id";
    public static final String COL_AMBIENTE_DESCRICAO = "descricao";
    public static final String COL_AMBIENTE_LARGURA = "largura";
    public static final String COL_AMBIENTE_COMPRIMENTO = "comprimento";
    public static final String COL_AMBIENTE_PONTOS_X = "pontos_x";
    public static final String COL_AMBIENTE_PONTOS_Y = "pontos_y";
    public static final String COL_AMBIENTE_STATUS = "status";

    // ... e assim por diante para outras tabelas, se necessário.

    // Declarações SQL para criar tabelas
    private static final String CREATE_TABLE_USUARIO = "CREATE TABLE " + TABLE_USUARIO + "("
            + COL_USUARIO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COL_USUARIO_NOME + " TEXT,"
            + COL_USUARIO_APELIDO + " TEXT,"
            + COL_USUARIO_STATUS + " INTEGER" + ")";

    private static final String CREATE_TABLE_AMBIENTE = "CREATE TABLE " + TABLE_AMBIENTE + "("
            + COL_AMBIENTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COL_AMBIENTE_DESCRICAO + " TEXT,"
            + COL_AMBIENTE_LARGURA + " REAL,"
            + COL_AMBIENTE_COMPRIMENTO + " REAL,"
            + COL_AMBIENTE_PONTOS_X + " TEXT,"
            + COL_AMBIENTE_PONTOS_Y + " TEXT,"
            + COL_AMBIENTE_STATUS + " INTEGER" + ")";

    private static final String CREATE_TABLE_PONTO = "CREATE TABLE " + TABLE_PONTO + "(id INTEGER PRIMARY KEY AUTOINCREMENT, id_ambiente INTEGER, numero INTEGER, posicao_x REAL, posicao_y REAL, status INTEGER, FOREIGN KEY(id_ambiente) REFERENCES ambiente(id))";
    private static final String CREATE_TABLE_LEITURA = "CREATE TABLE " + TABLE_LEITURA + "(id INTEGER PRIMARY KEY AUTOINCREMENT, id_ambiente INTEGER, id_ponto INTEGER, tempo_de_leitura INTEGER, data TEXT, status INTEGER, FOREIGN KEY(id_ambiente) REFERENCES ambiente(id), FOREIGN KEY(id_ponto) REFERENCES ponto(id))";
    private static final String CREATE_TABLE_LINHA = "CREATE TABLE " + TABLE_LINHA + "(id INTEGER PRIMARY KEY AUTOINCREMENT, id_leitura INTEGER, linha TEXT, situacao TEXT, FOREIGN KEY(id_leitura) REFERENCES leitura(id))";

    // ATUALIZADA: Declaração SQL para a tabela CSI_DATA (baseada no script Python)
    private static final String CREATE_TABLE_CSI_DATA = "CREATE TABLE csi_data ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "data_hora TEXT,"
            + "cenario TEXT,"
            + "type TEXT,"
            + "seq INTEGER,"
            + "mac TEXT,"
            + "rssi INTEGER,"
            + "rate REAL,"
            + "sig_mode INTEGER,"
            + "mcs INTEGER,"
            + "bandwidth INTEGER,"
            + "smoothing INTEGER,"
            + "not_sounding INTEGER,"
            + "aggregation INTEGER,"
            + "stbc INTEGER,"
            + "fec_coding INTEGER,"
            + "sgi INTEGER,"
            + "noise_floor INTEGER,"
            + "ampdu_cnt INTEGER,"
            + "channel INTEGER,"
            + "secondary_channel INTEGER,"
            + "local_timestamp INTEGER,"
            + "ant INTEGER,"
            + "sig_len INTEGER,"
            + "rx_state INTEGER,"
            + "len INTEGER,"
            + "first_word INTEGER,"
            + "data TEXT"
            + ")";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USUARIO);
        db.execSQL(CREATE_TABLE_AMBIENTE);
        db.execSQL(CREATE_TABLE_PONTO);
        db.execSQL(CREATE_TABLE_LEITURA);
        db.execSQL(CREATE_TABLE_LINHA);
        db.execSQL(CREATE_TABLE_CSI_DATA);
        Log.d("DatabaseHelper", "Todas as tabelas criadas com sucesso.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USUARIO);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AMBIENTE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PONTO);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LEITURA);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LINHA);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CSI_DATA);
        onCreate(db);
        Log.d("DatabaseHelper", "Banco de dados atualizado. Tabelas recriadas.");
    }

    // --- Métodos CRUD para a tabela USUARIO ---

    public long addUsuario(String nome, String apelido, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USUARIO_NOME, nome);
        values.put(COL_USUARIO_APELIDO, apelido);
        values.put(COL_USUARIO_STATUS, status);
        long id = db.insert(TABLE_USUARIO, null, values);
        db.close();
        return id;
    }

    public List<Usuario> getAllUsuarios() {
        List<Usuario> userList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_USUARIO;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_USUARIO_ID));
                String nome = cursor.getString(cursor.getColumnIndexOrThrow(COL_USUARIO_NOME));
                String apelido = cursor.getString(cursor.getColumnIndexOrThrow(COL_USUARIO_APELIDO));
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(COL_USUARIO_STATUS));
                userList.add(new Usuario(id, nome, apelido, status));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return userList;
    }

    public int updateUsuario(Usuario usuario) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USUARIO_NOME, usuario.getNome());
        values.put(COL_USUARIO_APELIDO, usuario.getApelido());
        values.put(COL_USUARIO_STATUS, usuario.getStatus());

        int rowsAffected = db.update(TABLE_USUARIO, values, COL_USUARIO_ID + " = ?",
                new String[]{String.valueOf(usuario.getId())});
        db.close();
        Log.d("DatabaseHelper", "Usuário atualizado: ID " + usuario.getId() + ", Linhas afetadas: " + rowsAffected);
        return rowsAffected;
    }

    public int deleteUsuario(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_USUARIO, COL_USUARIO_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
        Log.d("DatabaseHelper", "Usuário excluído: ID " + id + ", Linhas afetadas: " + rowsAffected);
        return rowsAffected;
    }

    // --- Métodos CRUD para outras tabelas ---

    public long addAmbiente(String descricao, double largura, double comprimento, List<Double> pontosX, List<Double> pontosY, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_AMBIENTE_DESCRICAO, descricao);
        values.put(COL_AMBIENTE_LARGURA, largura);
        values.put(COL_AMBIENTE_COMPRIMENTO, comprimento);
        values.put(COL_AMBIENTE_PONTOS_X, new JSONArray(pontosX).toString());
        values.put(COL_AMBIENTE_PONTOS_Y, new JSONArray(pontosY).toString());
        values.put(COL_AMBIENTE_STATUS, status);
        long id = db.insert(TABLE_AMBIENTE, null, values);
        db.close();
        return id;
    }

    public long addPonto(int idAmbiente, int numero, double posicaoX, double posicaoY, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id_ambiente", idAmbiente);
        values.put("numero", numero);
        values.put("posicao_x", posicaoX);
        values.put("posicao_y", posicaoY);
        values.put("status", status);
        long id = db.insert(TABLE_PONTO, null, values);
        db.close();
        return id;
    }

    public long addLeitura(int idAmbiente, int idPonto, int tempoDeLeitura, String data, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id_ambiente", idAmbiente);
        values.put("id_ponto", idPonto);
        values.put("tempo_de_leitura", tempoDeLeitura);
        values.put("data", data);
        values.put("status", status);
        long id = db.insert(TABLE_LEITURA, null, values);
        db.close();
        return id;
    }

    public long addLinha(int idLeitura, String linha, String situacao) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id_leitura", idLeitura);
        values.put("linha", linha);
        values.put("situacao", situacao);
        long id = db.insert(TABLE_LINHA, null, values);
        db.close();
        return id;
    }


    // --- Método CRUD para a nova tabela CSI_DATA ---

    public long addCsiData(String dataHora, String cenario, String[] csiParts) {
        if (csiParts == null || csiParts.length != 25) {
            Log.e("DatabaseHelper", "Array de partes CSI inválido. Esperado 25, recebido " + (csiParts != null ? csiParts.length : 0));
            return -1;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            values.put("data_hora", dataHora);
            values.put("cenario", cenario);
            values.put("type", csiParts[0]);
            values.put("seq", Integer.parseInt(csiParts[1]));
            values.put("mac", csiParts[2]);
            values.put("rssi", Integer.parseInt(csiParts[3]));
            values.put("rate", Float.parseFloat(csiParts[4]));
            values.put("sig_mode", Integer.parseInt(csiParts[5]));
            values.put("mcs", Integer.parseInt(csiParts[6]));
            values.put("bandwidth", Integer.parseInt(csiParts[7]));
            values.put("smoothing", Integer.parseInt(csiParts[8]));
            values.put("not_sounding", Integer.parseInt(csiParts[9]));
            values.put("aggregation", Integer.parseInt(csiParts[10]));
            values.put("stbc", Integer.parseInt(csiParts[11]));
            values.put("fec_coding", Integer.parseInt(csiParts[12]));
            values.put("sgi", Integer.parseInt(csiParts[13]));
            values.put("noise_floor", Integer.parseInt(csiParts[14]));
            values.put("ampdu_cnt", Integer.parseInt(csiParts[15]));
            values.put("channel", Integer.parseInt(csiParts[16]));
            values.put("secondary_channel", Integer.parseInt(csiParts[17]));
            values.put("local_timestamp", Long.parseLong(csiParts[18]));
            values.put("ant", Integer.parseInt(csiParts[19]));
            values.put("sig_len", Integer.parseInt(csiParts[20]));
            values.put("rx_state", Integer.parseInt(csiParts[21]));
            values.put("len", Integer.parseInt(csiParts[22]));
            values.put("first_word", Integer.parseInt(csiParts[23]));
            values.put("data", csiParts[24].replace("\"", ""));
        } catch (NumberFormatException e) {
            Log.e("DatabaseHelper", "Erro de formatação de número ao parsear dados CSI: " + e.getMessage());
            db.close();
            return -1;
        }

        long id = db.insert(TABLE_CSI_DATA, null, values);
        db.close();
        if (id == -1) {
            Log.e("DatabaseHelper", "Falha ao inserir dados CSI no banco de dados.");
        }
        return id;
    }
}