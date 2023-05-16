# Generated by Django 4.1.3 on 2023-05-16 09:25

from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("story_graph", "0013_populate_slug_display_name"),
    ]

    operations = [
        migrations.AlterField(
            model_name="graph",
            name="slug_name",
            field=models.SlugField(
                unique=True,
            ),
        ),
    ]
