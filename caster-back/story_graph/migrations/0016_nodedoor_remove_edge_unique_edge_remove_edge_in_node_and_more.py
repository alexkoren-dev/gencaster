# Generated by Django 4.2.4 on 2023-08-30 22:53
#
# The migration transforms the old Node - Edge - Node
# connection scheme to a Node - NodeExit - Edge - NodeExit - Node
# scheme which allows to have multiple outputs of a Node
# which is necessary to reflect visuals programming exits

import uuid

import django.db.models.deletion
from django.db import migrations, models


def edge_migration(apps, schema_editor):
    Edge = apps.get_model("story_graph", "Edge")
    NodeDoor = apps.get_model("story_graph", "NodeDoor")
    Node = apps.get_model("story_graph", "Node")

    # create default in- and out door for every node
    for node in Node.objects.all():
        # hardcoded from enums as import of it is not available
        for t in ["input", "output"]:
            NodeDoor.objects.create(
                door_type=t,
                node=node,
                name="default",
                is_default=True,
                code="",
            )

    # transfer the node-connecting edge to a
    # door-connecting edge by using the default doors
    for edge in Edge.objects.all():
        in_door = NodeDoor.objects.get(
            # pay attention - switcheroo!
            #
            # old (focussed on edge terminology)
            # Node_A --in_node--> edge --out_node--> Node_B
            #
            # new
            # Node_A --out_door --> edge --in_door--> Node_B
            node=edge.out_node,
            door_type="input",
            is_default=True,
        )
        out_door = NodeDoor.objects.get(
            node=edge.in_node,
            door_type="output",
            is_default=True,
        )
        edge.in_node_door = in_door
        edge.out_node_door = out_door
        edge.save()


def reverse_edge_migration(apps, schema_editor):
    Edge = apps.get_model("story_graph", "Edge")
    # transfer each node door to a edge
    for edge in Edge.objects.all():
        # remember the switcheroo
        edge.out_node = edge.in_node_door.node
        edge.in_node = edge.out_node_door.node
        edge.save()


class Migration(migrations.Migration):
    dependencies = [
        ("story_graph", "0015_alter_graph_about_text_alter_graph_end_text_and_more"),
    ]

    operations = [
        migrations.CreateModel(
            name="NodeDoor",
            fields=[
                (
                    "uuid",
                    models.UUIDField(
                        default=uuid.uuid4,
                        editable=False,
                        primary_key=True,
                        serialize=False,
                        unique=True,
                    ),
                ),
                (
                    "door_type",
                    models.CharField(
                        choices=[("input", "Input"), ("output", "output")],
                        default="output",
                        max_length=20,
                    ),
                ),
                ("name", models.CharField(max_length=512)),
                ("order", models.IntegerField(default=0)),
                ("is_default", models.BooleanField(default=False)),
                ("code", models.TextField(default="")),
            ],
            options={
                "verbose_name": "Node exit",
                "verbose_name_plural": "Node exits",
                "ordering": ["node", "is_default", "order", "name"],
            },
        ),
        migrations.RemoveConstraint(
            model_name="edge",
            name="unique_edge",
        ),
        migrations.AlterField(
            model_name="graph",
            name="about_text",
            field=models.TextField(
                blank=True,
                default="",
                help_text="Text about the graph which can be accessed during a stream - only if this is set",
                verbose_name="About text (markdown)",
            ),
        ),
        migrations.AlterField(
            model_name="graph",
            name="end_text",
            field=models.TextField(
                blank=True,
                default="",
                help_text="Text which will be displayed at the end of a stream",
                verbose_name="End text (markdown)",
            ),
        ),
        migrations.AlterField(
            model_name="graph",
            name="slug_name",
            field=models.SlugField(
                help_text="Will be used as a URL",
                max_length=256,
                unique=True,
                verbose_name="Slug name",
            ),
        ),
        migrations.AlterField(
            model_name="graph",
            name="start_text",
            field=models.TextField(
                blank=True,
                default="",
                help_text="Text about the graph which will be displayed at the start of a stream - only if this is set",
                verbose_name="Start text (markdown)",
            ),
        ),
        migrations.AddField(
            model_name="nodedoor",
            name="node",
            field=models.ForeignKey(
                on_delete=django.db.models.deletion.CASCADE, to="story_graph.node"
            ),
        ),
        migrations.AddField(
            model_name="edge",
            name="in_node_door",
            field=models.ForeignKey(
                null=True,
                on_delete=django.db.models.deletion.CASCADE,
                related_name="in_edges",
                to="story_graph.nodedoor",
            ),
        ),
        migrations.AddField(
            model_name="edge",
            name="out_node_door",
            field=models.ForeignKey(
                null=True,
                on_delete=django.db.models.deletion.CASCADE,
                related_name="out_edges",
                to="story_graph.nodedoor",
            ),
        ),
        migrations.AddConstraint(
            model_name="edge",
            constraint=models.UniqueConstraint(
                fields=("in_node_door", "out_node_door"), name="unique_edge"
            ),
        ),
        migrations.AddConstraint(
            model_name="nodedoor",
            constraint=models.UniqueConstraint(
                condition=models.Q(("is_default", True)),
                fields=("node", "door_type"),
                name="unique_default_per_type_and_node",
            ),
        ),
        migrations.RunPython(
            code=edge_migration,
            reverse_code=reverse_edge_migration,
        ),
        migrations.RemoveField(
            model_name="edge",
            name="in_node",
        ),
        migrations.RemoveField(
            model_name="edge",
            name="out_node",
        ),
    ]