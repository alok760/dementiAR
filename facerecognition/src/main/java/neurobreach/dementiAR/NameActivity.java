package neurobreach.dementiAR;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.opencv.neurobreach.dementiAR.R;

public class NameActivity extends AppCompatActivity {

    public static SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_name);

        final EditText name = (EditText) findViewById(R.id.name);
        Button nextButton = (Button) findViewById(R.id.nextButton);
        final EditText details= (EditText) findViewById(R.id.relation);


        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!name.getText().toString().equals("")||!details.getText().toString().equals("")) {
                    Context context =getApplicationContext();

                     sharedPref = context.getSharedPreferences(
                            "MyPref", Context.MODE_PRIVATE);

                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(name.getText().toString(), details.getText().toString());
                    editor.commit();

                    Intent intent = new Intent(NameActivity.this, Training.class);
                    intent.putExtra("name", name.getText().toString().trim());
                    intent.putExtra("relation",details.getText().toString().trim());
                    startActivity(intent);
                }
                else {
                    Toast.makeText(NameActivity.this, "Please enter the name and relation", Toast.LENGTH_LONG).show();
                }
            }
        });

    }
}
