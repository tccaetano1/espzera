package com.example.espzera;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class UsuarioManagerActivity extends AppCompatActivity
        implements UsuarioAdapter.OnUserActionListener, AddEditUserDialogFragment.OnUserSavedListener {

    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private UsuarioAdapter adapter;
    private List<Usuario> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuario_manager);

        // Configura a barra de ação (opcional, para exibir o botão Voltar)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.usuario_manager_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Habilita o botão Voltar
        }

        dbHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.recycler_view_users);
        Button buttonNewUser = findViewById(R.id.button_new_user);

        userList = new ArrayList<>();
        adapter = new UsuarioAdapter(userList, this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Carrega os usuários na inicialização
        loadUsers();

        buttonNewUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddEditUserDialog(null); // Passa null para indicar novo usuário
            }
        });
    }

    /**
     * Carrega todos os usuários do banco de dados e atualiza o RecyclerView.
     */
    private void loadUsers() {
        userList = dbHelper.getAllUsuarios(); // Obtém a lista atualizada de usuários
        adapter.updateUsers(userList); // Notifica o adaptador para atualizar a UI
        Log.d("UsuarioManagerActivity", "Usuários carregados: " + userList.size()); // Log para depuração
        if (userList.isEmpty()) {
            Toast.makeText(this, "Nenhum usuário cadastrado.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Exibe o diálogo para adicionar ou editar um usuário.
     * @param usuario O usuário a ser editado, ou null para adicionar um novo.
     */
    private void showAddEditUserDialog(Usuario usuario) {
        AddEditUserDialogFragment dialogFragment;
        if (usuario == null) {
            dialogFragment = AddEditUserDialogFragment.newInstance();
        } else {
            dialogFragment = AddEditUserDialogFragment.newInstance(usuario);
        }
        dialogFragment.show(getSupportFragmentManager(), AddEditUserDialogFragment.TAG);
    }

    // --- Implementação da interface OnUserActionListener (para botões Alterar/Excluir) ---

    @Override
    public void onEditClick(Usuario usuario) {
        // Quando o botão "Alterar" é clicado, abre o modal pré-preenchido
        showAddEditUserDialog(usuario);
    }

    @Override
    public void onDeleteClick(Usuario usuario) {
        // Confirmação antes de excluir
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(getString(R.string.confirm_delete_message, usuario.getNome()))
                .setPositiveButton(R.string.yes_button, (dialog, which) -> {
                    int rowsAffected = dbHelper.deleteUsuario(usuario.getId());
                    if (rowsAffected > 0) {
                        Toast.makeText(UsuarioManagerActivity.this, "Usuário excluído: " + usuario.getNome(), Toast.LENGTH_SHORT).show();
                        loadUsers(); // Recarrega a lista após a exclusão
                    } else {
                        Toast.makeText(UsuarioManagerActivity.this, "Erro ao excluir usuário.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.no_button, null)
                .show();
    }

    // --- Implementação da interface OnUserSavedListener (para o diálogo) ---

    @Override
    public void onUserSaved() {
        // Este método é chamado pelo diálogo quando um usuário é adicionado ou atualizado
        loadUsers(); // Recarrega a lista de usuários para mostrar as alterações
    }
}