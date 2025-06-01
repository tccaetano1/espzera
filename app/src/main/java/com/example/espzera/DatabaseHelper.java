package com.example.espzera; // ATENÇÃO: VERIFIQUE SE ESTE É O NOME DO SEU PACOTE REAL!

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
    private static final int DATABASE_VERSION = 1; // Mantenha a versão 1, se não houver outras mudanças de esquema

    // Nomes das tabelas
    public static final String TABLE_USUARIO = "usuario";
    public static final String TABLE_AMBIENTE = "ambiente";
    public static final String TABLE_PONTO = "ponto";
    public static final String TABLE_LEITURA = "leitura";
    public static final String TABLE_LINHA = "linha";
    public static final String TABLE_CSI_DATA = "csi_data"; // NOVA TABELA CSI

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
    public static final String COL_AMBIENTE_PONTOS_X = "pontos_x"; // JSON string
    public static final String COL_AMBIENTE_PONTOS_Y = "pontos_y"; // JSON string
    public static final String COL_AMBIENTE_STATUS = "status";

    // Colunas da tabela PONTO
    public static final String COL_PONTO_ID = "id";
    public static final String COL_PONTO_ID_AMBIENTE = "id_ambiente";
    public static final String COL_PONTO_NUMERO = "numero";
    public static final String COL_PONTO_POSICAO_X = "posicao_x";
    public static final String COL_PONTO_POSICAO_Y = "posicao_y";
    public static final String COL_PONTO_STATUS = "status";

    // Colunas da tabela LEITURA
    public static final String COL_LEITURA_ID = "id";
    public static final String COL_LEITURA_ID_AMBIENTE = "id_ambiente";
    public static final String COL_LEITURA_ID_PONTO = "id_ponto";
    public static final String COL_LEITURA_TEMPO_DE_LEITURA = "tempo_de_leitura";
    public static final String COL_LEITURA_DATA = "data";
    public static final String COL_LEITURA_STATUS = "status";

    // Colunas da tabela LINHA
    public static final String COL_LINHA_ID = "id";
    public static final String COL_LINHA_ID_LEITURA = "id_leitura";
    public static final String COL_LINHA_LINHA = "linha";
    public static final String COL_LINHA_SITUACAO = "situacao";

    // NOVAS Colunas da tabela CSI_DATA
    public static final String COL_CSI_DATA_ID = "id";
    public static final String COL_CSI_DATA_ID_LEITURA = "id_leitura"; // Chave estrangeira para LEITURA
    public static final String COL_CSI_DATA_TIMESTAMP = "timestamp"; // Carimbo de data/hora do dado CSI
    public static final String COL_CSI_DATA_PAYLOAD = "csi_payload"; // Os dados CSI em si (TEXT ou BLOB)


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
            + COL_AMBIENTE_PONTOS_X + " TEXT," // JSON string
            + COL_AMBIENTE_PONTOS_Y + " TEXT," // JSON string
            + COL_AMBIENTE_STATUS + " INTEGER" + ")";

    private static final String CREATE_TABLE_PONTO = "CREATE TABLE " + TABLE_PONTO + "("
            + COL_PONTO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COL_PONTO_ID_AMBIENTE + " INTEGER,"
            + COL_PONTO_NUMERO + " INTEGER,"
            + COL_PONTO_POSICAO_X + " REAL,"
            + COL_PONTO_POSICAO_Y + " REAL,"
            + COL_PONTO_STATUS + " INTEGER,"
            + "FOREIGN KEY(" + COL_PONTO_ID_AMBIENTE + ") REFERENCES " + TABLE_AMBIENTE + "(" + COL_AMBIENTE_ID + ")" + ")";

    private static final String CREATE_TABLE_LEITURA = "CREATE TABLE " + TABLE_LEITURA + "("
            + COL_LEITURA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COL_LEITURA_ID_AMBIENTE + " INTEGER,"
            + COL_LEITURA_ID_PONTO + " INTEGER,"
            + COL_LEITURA_TEMPO_DE_LEITURA + " INTEGER,"
            + COL_LEITURA_DATA + " TEXT,"
            + COL_LEITURA_STATUS + " INTEGER,"
            + "FOREIGN KEY(" + COL_LEITURA_ID_AMBIENTE + ") REFERENCES " + TABLE_AMBIENTE + "(" + COL_AMBIENTE_ID + "),"
            + "FOREIGN KEY(" + COL_LEITURA_ID_PONTO + ") REFERENCES " + TABLE_PONTO + "(" + COL_PONTO_ID + ")" + ")";

    private static final String CREATE_TABLE_LINHA = "CREATE TABLE " + TABLE_LINHA + "("
            + COL_LINHA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COL_LINHA_ID_LEITURA + " INTEGER,"
            + COL_LINHA_LINHA + " TEXT,"
            + COL_LINHA_SITUACAO + " TEXT,"
            + "FOREIGN KEY(" + COL_LINHA_ID_LEITURA + ") REFERENCES " + TABLE_LEITURA + "(" + COL_LEITURA_ID + ")" + ")";

    // NOVA DECLARAÇÃO SQL para a tabela CSI_DATA
    private static final String CREATE_TABLE_CSI_DATA = "CREATE TABLE " + TABLE_CSI_DATA + "("
            + COL_CSI_DATA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COL_CSI_DATA_ID_LEITURA + " INTEGER,"
            + COL_CSI_DATA_TIMESTAMP + " TEXT," // Formato "YYYY-MM-DD HH:MM:SS.SSS" para precisão
            + COL_CSI_DATA_PAYLOAD + " TEXT," // Os dados CSI em formato de string (ex: JSON)
            + "FOREIGN KEY(" + COL_CSI_DATA_ID_LEITURA + ") REFERENCES " + TABLE_LEITURA + "(" + COL_LEITURA_ID + ")" + ")";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Executa as declarações SQL para criar todas as tabelas
        db.execSQL(CREATE_TABLE_USUARIO);
        db.execSQL(CREATE_TABLE_AMBIENTE);
        db.execSQL(CREATE_TABLE_PONTO);
        db.execSQL(CREATE_TABLE_LEITURA);
        db.execSQL(CREATE_TABLE_LINHA);
        db.execSQL(CREATE_TABLE_CSI_DATA); // Cria a nova tabela CSI
        Log.d("DatabaseHelper", "Todas as tabelas criadas com sucesso.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Em caso de atualização, descarta as tabelas existentes e as recria
        // ATENÇÃO: Em um aplicativo de produção, você faria um ALTER TABLE para preservar dados.
        // Para fins de desenvolvimento, dropar e recriar é mais simples.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USUARIO);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AMBIENTE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PONTO);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LEITURA);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LINHA);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CSI_DATA); // Dropar a nova tabela CSI também
        onCreate(db);
        Log.d("DatabaseHelper", "Banco de dados atualizado. Tabelas descartadas e recriadas.");
    }

    // --- Métodos CRUD para a tabela USUARIO ---

    /**
     * Adiciona um novo usuário ao banco de dados.
     * @param nome O nome do usuário.
     * @param apelido O apelido do usuário.
     * @param status O status do usuário (0 ou 1).
     * @return O ID da nova linha inserida, ou -1 se ocorrer um erro.
     */
    public long addUsuario(String nome, String apelido, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USUARIO_NOME, nome);
        values.put(COL_USUARIO_APELIDO, apelido);
        values.put(COL_USUARIO_STATUS, status);

        long id = db.insert(TABLE_USUARIO, null, values);
        db.close();
        Log.d("DatabaseHelper", "Usuário adicionado: " + nome + ", ID: " + id);
        return id;
    }

    /**
     * Obtém todos os usuários do banco de dados.
     * @return Uma lista de objetos Usuario.
     */
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
        Log.d("DatabaseHelper", "Usuários recuperados: " + userList.size());
        return userList;
    }

    /**
     * Atualiza um usuário existente no banco de dados.
     * @param usuario O objeto Usuario com os dados atualizados.
     * @return O número de linhas afetadas.
     */
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

    /**
     * Exclui um usuário do banco de dados.
     * @param id O ID do usuário a ser excluído.
     * @return O número de linhas afetadas.
     */
    public int deleteUsuario(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_USUARIO, COL_USUARIO_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
        Log.d("DatabaseHelper", "Usuário excluído: ID " + id + ", Linhas afetadas: " + rowsAffected);
        return rowsAffected;
    }

    // --- Métodos CRUD para a tabela AMBIENTE ---

    /**
     * Adiciona um novo ambiente ao banco de dados.
     * @param descricao A descrição do ambiente.
     * @param largura A largura do ambiente.
     * @param comprimento O comprimento do ambiente.
     * @param pontosX Uma lista de coordenadas X dos pontos (será serializada para JSON).
     * @param pontosY Uma lista de coordenadas Y dos pontos (será serializada para JSON).
     * @param status O status do ambiente.
     * @return O ID da nova linha inserida, ou -1 se ocorrer um erro.
     */
    public long addAmbiente(String descricao, double largura, double comprimento,
                            List<Double> pontosX, List<Double> pontosY, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_AMBIENTE_DESCRICAO, descricao);
        values.put(COL_AMBIENTE_LARGURA, largura);
        values.put(COL_AMBIENTE_COMPRIMENTO, comprimento);

        // Serializa listas de pontos para JSON
        values.put(COL_AMBIENTE_PONTOS_X, new JSONArray(pontosX).toString());
        values.put(COL_AMBIENTE_PONTOS_Y, new JSONArray(pontosY).toString());
        values.put(COL_AMBIENTE_STATUS, status);

        long id = db.insert(TABLE_AMBIENTE, null, values);
        db.close();
        Log.d("DatabaseHelper", "Ambiente adicionado: " + descricao + ", ID: " + id);
        return id;
    }

    /**
     * Obtém um ambiente do banco de dados com base na largura e comprimento.
     * @param largura A largura do ambiente.
     * @param comprimento O comprimento do ambiente.
     * @return O objeto Ambiente correspondente, ou null se não encontrado.
     */
    public Ambiente getAmbiente(double largura, double comprimento) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_AMBIENTE,
                new String[]{COL_AMBIENTE_ID, COL_AMBIENTE_DESCRICAO, COL_AMBIENTE_LARGURA,
                        COL_AMBIENTE_COMPRIMENTO, COL_AMBIENTE_PONTOS_X, COL_AMBIENTE_PONTOS_Y,
                        COL_AMBIENTE_STATUS},
                COL_AMBIENTE_LARGURA + " = ? AND " + COL_AMBIENTE_COMPRIMENTO + " = ?",
                new String[]{String.valueOf(largura), String.valueOf(comprimento)},
                null, null, null, "1"); // Limita a 1 resultado

        Ambiente ambiente = null;
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_AMBIENTE_ID));
            String descricao = cursor.getString(cursor.getColumnIndexOrThrow(COL_AMBIENTE_DESCRICAO));
            double ambLargura = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMBIENTE_LARGURA));
            double ambComprimento = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMBIENTE_COMPRIMENTO));
            String pontosXJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_AMBIENTE_PONTOS_X));
            String pontosYJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_AMBIENTE_PONTOS_Y));
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(COL_AMBIENTE_STATUS));

            List<Double> pontosX = new ArrayList<>();
            List<Double> pontosY = new ArrayList<>();
            try {
                if (pontosXJson != null) {
                    JSONArray jsonArrayX = new JSONArray(pontosXJson);
                    for (int i = 0; i < jsonArrayX.length(); i++) {
                        pontosX.add(jsonArrayX.getDouble(i));
                    }
                }
                if (pontosYJson != null) {
                    JSONArray jsonArrayY = new JSONArray(pontosYJson);
                    for (int i = 0; i < jsonArrayY.length(); i++) {
                        pontosY.add(jsonArrayY.getDouble(i));
                    }
                }
            } catch (JSONException e) {
                Log.e("DatabaseHelper", "Erro ao analisar JSON de pontos para Ambiente: " + e.getMessage());
            }
            ambiente = new Ambiente(id, descricao, ambLargura, ambComprimento, pontosX, pontosY, status);
        }
        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return ambiente;
    }

    /**
     * Obtém um ambiente existente ou adiciona um novo ao banco de dados.
     * @param descricao A descrição do ambiente.
     * @param largura A largura do ambiente.
     * @param comprimento O comprimento do ambiente.
     * @param status O status do ambiente.
     * @return O ID do ambiente existente ou recém-criado, ou -1 se ocorrer um erro.
     */
    public long getOrAddAmbiente(String descricao, double largura, double comprimento, int status) {
        Ambiente existingAmbiente = getAmbiente(largura, comprimento);
        if (existingAmbiente != null) {
            Log.d("DatabaseHelper", "Ambiente existente encontrado: ID " + existingAmbiente.getId());
            return existingAmbiente.getId();
        } else {
            // Se não existir, adiciona um novo ambiente. Pontos X e Y podem ser vazios para este contexto.
            Log.d("DatabaseHelper", "Adicionando novo ambiente: " + descricao);
            return addAmbiente(descricao, largura, comprimento, new ArrayList<>(), new ArrayList<>(), status);
        }
    }

    // Método para obter ambientes (exemplo) - Mantido, mas getAmbiente acima é mais específico
    public List<String> getAllAmbientes() {
        List<String> ambienteList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_AMBIENTE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_AMBIENTE_ID));
                String descricao = cursor.getString(cursor.getColumnIndexOrThrow(COL_AMBIENTE_DESCRICAO));
                double largura = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMBIENTE_LARGURA));
                double comprimento = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMBIENTE_COMPRIMENTO));
                String pontosXJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_AMBIENTE_PONTOS_X));
                String pontosYJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_AMBIENTE_PONTOS_Y));
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(COL_AMBIENTE_STATUS));

                List<Double> pontosX = new ArrayList<>();
                List<Double> pontosY = new ArrayList<>();
                try {
                    if (pontosXJson != null) {
                        JSONArray jsonArrayX = new JSONArray(pontosXJson);
                        for (int i = 0; i < jsonArrayX.length(); i++) {
                            pontosX.add(jsonArrayX.getDouble(i));
                        }
                    }
                    if (pontosYJson != null) {
                        JSONArray jsonArrayY = new JSONArray(pontosYJson);
                        for (int i = 0; i < jsonArrayY.length(); i++) {
                            pontosY.add(jsonArrayY.getDouble(i));
                        }
                    }
                } catch (JSONException e) {
                    Log.e("DatabaseHelper", "Erro ao analisar JSON de pontos: " + e.getMessage());
                }

                ambienteList.add("ID: " + id + ", Descrição: " + descricao + ", Largura: " + largura +
                        ", Comprimento: " + comprimento + ", Pontos X: " + pontosX.toString() +
                        ", Pontos Y: " + pontosY.toString() + ", Status: " + status);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        Log.d("DatabaseHelper", "Ambientes recuperados: " + ambienteList.size());
        return ambienteList;
    }

    // --- Métodos CRUD para a tabela PONTO ---

    /**
     * Adiciona um novo ponto ao banco de dados.
     * @param idAmbiente O ID do ambiente ao qual o ponto pertence.
     * @param numero O número do ponto.
     * @param posicaoX A posição X do ponto.
     * @param posicaoY A posição Y do ponto.
     * @param status O status do ponto.
     * @return O ID da nova linha inserida, ou -1 se ocorrer um erro.
     */
    public long addPonto(int idAmbiente, int numero, double posicaoX, double posicaoY, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PONTO_ID_AMBIENTE, idAmbiente);
        values.put(COL_PONTO_NUMERO, numero);
        values.put(COL_PONTO_POSICAO_X, posicaoX);
        values.put(COL_PONTO_POSICAO_Y, posicaoY);
        values.put(COL_PONTO_STATUS, status);

        long id = db.insert(TABLE_PONTO, null, values);
        db.close();
        Log.d("DatabaseHelper", "Ponto adicionado: " + numero + ", ID: " + id);
        return id;
    }

    /**
     * Obtém um ponto do banco de dados com base no ID do ambiente e nas coordenadas.
     * @param idAmbiente O ID do ambiente ao qual o ponto pertence.
     * @param posicaoX A posição X do ponto.
     * @param posicaoY A posição Y do ponto.
     * @return O objeto Ponto correspondente, ou null se não encontrado.
     */
    public Ponto getPonto(int idAmbiente, double posicaoX, double posicaoY) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PONTO,
                new String[]{COL_PONTO_ID, COL_PONTO_ID_AMBIENTE, COL_PONTO_NUMERO,
                        COL_PONTO_POSICAO_X, COL_PONTO_POSICAO_Y, COL_PONTO_STATUS},
                COL_PONTO_ID_AMBIENTE + " = ? AND " + COL_PONTO_POSICAO_X + " = ? AND " + COL_PONTO_POSICAO_Y + " = ?",
                new String[]{String.valueOf(idAmbiente), String.valueOf(posicaoX), String.valueOf(posicaoY)},
                null, null, null, "1");

        Ponto ponto = null;
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PONTO_ID));
            int ambId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PONTO_ID_AMBIENTE));
            int numero = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PONTO_NUMERO));
            double pX = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PONTO_POSICAO_X));
            double pY = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PONTO_POSICAO_Y));
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PONTO_STATUS));
            ponto = new Ponto(id, ambId, numero, pX, pY, status);
        }
        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return ponto;
    }

    /**
     * Obtém um ponto existente ou adiciona um novo ao banco de dados.
     * @param idAmbiente O ID do ambiente ao qual o ponto pertence.
     * @param numero O número do ponto.
     * @param posicaoX A posição X do ponto.
     * @param posicaoY A posição Y do ponto.
     * @param status O status do ponto.
     * @return O ID do ponto existente ou recém-criado, ou -1 se ocorrer um erro.
     */
    public long getOrAddPonto(int idAmbiente, int numero, double posicaoX, double posicaoY, int status) {
        Ponto existingPonto = getPonto(idAmbiente, posicaoX, posicaoY);
        if (existingPonto != null) {
            Log.d("DatabaseHelper", "Ponto existente encontrado: ID " + existingPonto.getId());
            return existingPonto.getId();
        } else {
            Log.d("DatabaseHelper", "Adicionando novo ponto para Ambiente ID " + idAmbiente + ": (" + posicaoX + ", " + posicaoY + ")");
            return addPonto(idAmbiente, numero, posicaoX, posicaoY, status);
        }
    }


    // --- Métodos CRUD para a tabela LEITURA ---

    /**
     * Adiciona uma nova leitura ao banco de dados.
     * @param idAmbiente O ID do ambiente da leitura.
     * @param idPonto O ID do ponto da leitura.
     * @param tempoDeLeitura O tempo de leitura.
     * @param data A data da leitura (formato TEXT, ex: "YYYY-MM-DD HH:MM:SS").
     * @param status O status da leitura.
     * @return O ID da nova linha inserida, ou -1 se ocorrer um erro.
     */
    public long addLeitura(int idAmbiente, int idPonto, int tempoDeLeitura, String data, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_LEITURA_ID_AMBIENTE, idAmbiente);
        values.put(COL_LEITURA_ID_PONTO, idPonto);
        values.put(COL_LEITURA_TEMPO_DE_LEITURA, tempoDeLeitura);
        values.put(COL_LEITURA_DATA, data);
        values.put(COL_LEITURA_STATUS, status);

        long id = db.insert(TABLE_LEITURA, null, values);
        db.close();
        Log.d("DatabaseHelper", "Leitura adicionada: " + data + ", ID: " + id);
        return id;
    }

    // --- Métodos CRUD para a tabela LINHA ---

    /**
     * Adiciona uma nova linha ao banco de dados.
     * @param idLeitura O ID da leitura à qual a linha pertence.
     * @param linha O conteúdo da linha.
     * @param situacao A situação da linha.
     * @return O ID da nova linha inserida, ou -1 se ocorrer um erro.
     */
    public long addLinha(int idLeitura, String linha, String situacao) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_LINHA_ID_LEITURA, idLeitura);
        values.put(COL_LINHA_LINHA, linha);
        values.put(COL_LINHA_SITUACAO, situacao);

        long id = db.insert(TABLE_LINHA, null, values);
        db.close();
        Log.d("DatabaseHelper", "Linha adicionada: " + linha + ", ID: " + id);
        return id;
    }

    // --- NOVOS Métodos CRUD para a tabela CSI_DATA ---

    /**
     * Adiciona um novo registro de dados CSI ao banco de dados.
     * @param idLeitura O ID da leitura à qual este dado CSI pertence.
     * @param timestamp O carimbo de data/hora da coleta do CSI (formato TEXT).
     * @param csiPayload Os dados CSI em formato de string (ex: JSON).
     * @return O ID da nova linha inserida, ou -1 se ocorrer um erro.
     */
    public long addCsiData(int idLeitura, String timestamp, String csiPayload) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CSI_DATA_ID_LEITURA, idLeitura);
        values.put(COL_CSI_DATA_TIMESTAMP, timestamp);
        values.put(COL_CSI_DATA_PAYLOAD, csiPayload);

        long id = db.insert(TABLE_CSI_DATA, null, values);
        db.close();
        Log.d("DatabaseHelper", "CSI Data adicionada para Leitura ID " + idLeitura + ", ID: " + id);
        return id;
    }
}
