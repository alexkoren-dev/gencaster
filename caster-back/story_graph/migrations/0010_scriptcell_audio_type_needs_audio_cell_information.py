# Generated by Django 4.1.3 on 2023-03-14 19:19

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("story_graph", "0009_alter_scriptcell_cell_type_audiocell_and_more"),
    ]

    operations = [
        migrations.AddConstraint(
            model_name="scriptcell",
            constraint=models.CheckConstraint(
                check=models.Q(
                    models.Q(("audio_cell__isnull", False), ("cell_type", "audio")),
                    models.Q(("cell_type", "audio"), _negated=True),
                    _connector="OR",
                ),
                name="audio_type_needs_audio_cell_information",
            ),
        ),
    ]
