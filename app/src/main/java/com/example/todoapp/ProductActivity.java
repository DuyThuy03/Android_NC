package com.example.todoapp;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProductActivity extends AppCompatActivity {

    DatabaseReference mDatabase;
    RecyclerView recyclerView;
    ProductAdapter adapter;
    List<Product> productList;
    FloatingActionButton fabAdd;
    SearchView searchView;

    private static final String CHANNEL_ID = "product_channel";
    private static final String CHANNEL_NAME = "Thông báo sản phẩm";



    private FloatingActionButton fabThem;
    private Button btnSapXep;

    // Đối tượng Firebase
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product);
        recyclerView = findViewById(R.id.recyclerView);
        fabAdd = findViewById(R.id.fabAdd);
        searchView = findViewById(R.id.searchView);



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
        createNotificationChannel();
        // ------------------------------------

        // Khởi tạo Firebase Reference tham chiếu đến node "products"
        mDatabase = FirebaseDatabase.getInstance().getReference("products");



        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        productList = new ArrayList<>();


        adapter = new ProductAdapter(this, productList, id -> showDeleteDialog(id));
        recyclerView.setAdapter(adapter);

        loadProductsFromFirebase(""); // Load dữ liệu

        fabAdd.setOnClickListener(v -> showAddDialog());

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                loadProductsFromFirebase(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                loadProductsFromFirebase(newText);
                return false;
            }
        });
    }

    // Hàm đọc dữ liệu từ Firebase
    private void loadProductsFromFirebase(String keyword) {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Product product = data.getValue(Product.class);
                    if (product != null) {
                        product.setId(data.getKey()); // Lấy Key từ Firebase gán vào ID


                        if (keyword.isEmpty() || product.getName().toLowerCase().contains(keyword.toLowerCase())) {
                            productList.add(product);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProductActivity.this, "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null);
        builder.setView(view);

        EditText etName = view.findViewById(R.id.etName);
        EditText etPrice = view.findViewById(R.id.etPrice);
        EditText etDesc = view.findViewById(R.id.etDesc);

        builder.setTitle("Thêm sản phẩm mới");
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String name = etName.getText().toString();
            String priceStr = etPrice.getText().toString();
            String desc = etDesc.getText().toString();

            if (name.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Tạo ID mới bằng push()
            String newId = mDatabase.push().getKey();
            Product newProduct = new Product(newId, name, Double.parseDouble(priceStr), desc);

            // Đẩy lên Firebase
            if (newId != null) {
                mDatabase.child(newId).setValue(newProduct)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Thêm thành công!", Toast.LENGTH_SHORT).show();
                            sendNotification("Sản phẩm mới", "Đã thêm: " + name);
                            // Không cần gọi loadProducts() vì addValueEventListener tự động cập nhật
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    // Hàm xóa sản phẩm (Lưu ý tham số id là String)
    private void showDeleteDialog(String id) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận")
                .setMessage("Bạn có chắc muốn xóa sản phẩm này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    mDatabase.child(id).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã xóa!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Lỗi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ... Giữ nguyên phần createNotificationChannel và sendNotification
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void sendNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(1, builder.build());
        }
    }
}

